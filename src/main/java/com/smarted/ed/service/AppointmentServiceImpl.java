package com.smarted.ed.service;

import com.smarted.ed.dto.AppointmentRequest;
import com.smarted.ed.dto.AppointmentResponse;
import com.smarted.ed.entity.*;
import com.smarted.ed.enums.AppointmentStatus;
import com.smarted.ed.enums.PaymentStatus;
import com.smarted.ed.exception.ResourceNotFoundException;
import com.smarted.ed.repository.*;
import com.smarted.ed.repository.TransactionRepository;
import com.smarted.ed.configs.VNPayConfig;
import com.smarted.ed.utils.VNPayUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AppointmentServiceImpl implements AppointmentService {

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private TutorProfileRepository tutorProfileRepository;

    @Autowired
    private StudentProfileRepository studentProfileRepository;

    @Autowired
    private TutorSubjectRepository tutorSubjectRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EmailService emailService;

    @Autowired
    private VNPayConfig vnPayConfig;

    @Autowired
    private HttpServletRequest httpServletRequest;

    @Autowired
    private TransactionRepository transactionRepository;

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @Override
    @Transactional
    public AppointmentResponse createAppointment(Integer parentId, AppointmentRequest request) {
        // Validate student exists and belongs to the logged-in parent
        StudentProfile student = studentProfileRepository.findById(request.getStudentId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy hồ sơ học sinh"));

        if (!student.getParentId().equals(parentId)) {
            throw new IllegalArgumentException("Học sinh này không thuộc quyền quản lý của bạn");
        }

        // Validate tutor exists
        TutorProfile tutor = tutorProfileRepository.findById(request.getTutorId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy hồ sơ gia sư"));

        // Get tutor's rate for the subject
        TutorSubjectId compositeId = new TutorSubjectId(tutor.getUserId(), request.getSubjectId());
        TutorSubject tutorSubject = tutorSubjectRepository.findById(compositeId)
                .orElseThrow(() -> new ResourceNotFoundException("Môn học này chưa được gia sư đăng ký giảng dạy"));

        // Check for time overlap
        long overlapCount = appointmentRepository.countOverlappingAppointments(
                request.getTutorId(), request.getStartTime(), request.getEndTime());

        if (overlapCount > 0) {
            throw new IllegalArgumentException("Trùng lịch học của gia sư. Vui lòng chọn ca học hoặc giờ khác.");
        }

        // Compute price
        BigDecimal hourlyRate = tutorSubject.getHourlyRate();
        long minutes = Duration.between(request.getStartTime(), request.getEndTime()).toMinutes();
        BigDecimal hours = BigDecimal.valueOf(minutes).divide(BigDecimal.valueOf(60), 2, java.math.RoundingMode.HALF_UP);
        BigDecimal totalPrice = hourlyRate.multiply(hours);

        // Build appointment entity (PENDING and UNPAID initially)
        Appointment appointment = new Appointment();
        appointment.setStudentId(student.getId());
        appointment.setTutorId(tutor.getUserId());
        appointment.setSubjectId(request.getSubjectId());
        appointment.setStartTime(request.getStartTime());
        appointment.setEndTime(request.getEndTime());
        appointment.setStatus(AppointmentStatus.PENDING);
        appointment.setPaymentStatus(PaymentStatus.UNPAID);
        appointment.setTotalPrice(totalPrice);

        Appointment saved = appointmentRepository.save(appointment);

        // Generate VNPay payment URL
        String paymentUrl = "";
        try {
            long amountInVnd = totalPrice.setScale(0, java.math.RoundingMode.HALF_UP).longValue();
            String amountString = String.valueOf(amountInVnd * 100L);
            String clientIp = VNPayUtil.getIpAddress(httpServletRequest);

            Map<String, String> vnpParamsMap = vnPayConfig.getVNPayConfig(
                    saved.getId().toString(),
                    amountString,
                    clientIp
            );

            String queryUrl = VNPayUtil.getPaymentURL(vnpParamsMap, true);
            String hashData = VNPayUtil.getPaymentURL(vnpParamsMap, false);
            String vnpSecureHash = VNPayUtil.hmacSHA512(vnPayConfig.getSecretKey(), hashData);
            queryUrl += "&vnp_SecureHash=" + vnpSecureHash;
            paymentUrl = vnPayConfig.getVnp_PayUrl() + "?" + queryUrl;
        } catch (Exception e) {
            e.printStackTrace();
        }

        AppointmentResponse response = convertToDto(saved);
        response.setPaymentUrl(paymentUrl);
        return response;
    }

    @Override
    @Transactional
    public AppointmentResponse acceptAppointment(Integer tutorId, Integer appointmentId) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy lịch học"));

        if (!appointment.getTutorId().equals(tutorId)) {
            throw new IllegalArgumentException("Bạn không có quyền duyệt lịch học này");
        }

        if (appointment.getStatus() != AppointmentStatus.PENDING) {
            throw new IllegalArgumentException("Chỉ được duyệt lịch học ở trạng thái Chờ xác nhận");
        }

        appointment.setStatus(AppointmentStatus.CONFIRMED);
        Appointment saved = appointmentRepository.save(appointment);

        // Send email to parent
        sendStateChangeEmailToParent(saved, "đã được chấp nhận bởi gia sư", "Lịch học của bạn đã được gia sư chấp nhận - SmartEd");

        return convertToDto(saved);
    }

    @Override
    @Transactional
    public AppointmentResponse rejectAppointment(Integer tutorId, Integer appointmentId, String reason) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy lịch học"));

        if (!appointment.getTutorId().equals(tutorId)) {
            throw new IllegalArgumentException("Bạn không có quyền hủy lịch học này");
        }

        if (appointment.getStatus() != AppointmentStatus.PENDING && appointment.getStatus() != AppointmentStatus.CONFIRMED) {
            throw new IllegalArgumentException("Không thể hủy lịch học ở trạng thái hiện tại");
        }

        appointment.setStatus(AppointmentStatus.REJECTED);
        appointment.setCancellationReason(reason);
        Appointment saved = appointmentRepository.save(appointment);

        // Send email to parent
        String statusText = "đã bị từ chối/hủy bởi gia sư với lý do: " + (reason != null ? reason : "Không có lý do cụ thể");
        sendStateChangeEmailToParent(saved, statusText, "Lịch học của bạn đã bị hủy - SmartEd");

        return convertToDto(saved);
    }

    @Override
    @Transactional
    public AppointmentResponse completeAppointment(Integer tutorId, Integer appointmentId) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy lịch học"));

        if (!appointment.getTutorId().equals(tutorId)) {
            throw new IllegalArgumentException("Bạn không có quyền kết thúc lịch học này");
        }

        if (appointment.getStatus() != AppointmentStatus.CONFIRMED) {
            throw new IllegalArgumentException("Chỉ được kết thúc lịch học ở trạng thái Đã xác nhận");
        }

        if (LocalDateTime.now().isBefore(appointment.getEndTime())) {
            throw new IllegalArgumentException("Chỉ được hoàn thành lịch học sau khi ca dạy kết thúc (vượt quá " + appointment.getEndTime().format(DATE_TIME_FORMATTER) + ")");
        }

        appointment.setStatus(AppointmentStatus.COMPLETED);
        Appointment saved = appointmentRepository.save(appointment);

        // Send email to parent
        sendStateChangeEmailToParent(saved, "đã hoàn thành xuất sắc", "Buổi học của bạn đã hoàn thành - SmartEd");

        return convertToDto(saved);
    }

    @Override
    public org.springframework.data.domain.Page<AppointmentResponse> getAppointmentsForTutor(
            Integer tutorId, String status, String month, org.springframework.data.domain.Pageable pageable) {
        AppointmentStatus statusEnum = null;
        if (status != null && !status.trim().isEmpty()) {
            try {
                statusEnum = AppointmentStatus.valueOf(status.trim().toUpperCase());
            } catch (IllegalArgumentException e) {
                // ignore
            }
        }

        Integer filterMonth = null;
        Integer filterYear = null;
        if (month != null && !month.trim().isEmpty()) {
            String trimmed = month.trim();
            if (trimmed.contains("-")) {
                String[] parts = trimmed.split("-");
                if (parts.length >= 2) {
                    try {
                        filterYear = Integer.parseInt(parts[0]);
                        filterMonth = Integer.parseInt(parts[1]);
                    } catch (NumberFormatException e) { }
                }
            } else {
                try {
                    filterMonth = Integer.parseInt(trimmed);
                } catch (NumberFormatException e) { }
            }
        }

        return appointmentRepository.findForTutorWithFilters(tutorId, statusEnum, filterMonth, filterYear, pageable)
                .map(this::convertToDto);
    }

    @Override
    public org.springframework.data.domain.Page<AppointmentResponse> getAppointmentsForParent(
            Integer parentId, Integer studentId, String status, String month, org.springframework.data.domain.Pageable pageable) {
        List<StudentProfile> students = studentProfileRepository.findByParentId(parentId);
        if (students.isEmpty()) {
            return org.springframework.data.domain.Page.empty(pageable);
        }
        List<Integer> studentIds = students.stream().map(StudentProfile::getId).collect(Collectors.toList());

        AppointmentStatus statusEnum = null;
        if (status != null && !status.trim().isEmpty()) {
            try {
                statusEnum = AppointmentStatus.valueOf(status.trim().toUpperCase());
            } catch (IllegalArgumentException e) {
                // ignore
            }
        }

        Integer filterMonth = null;
        Integer filterYear = null;
        if (month != null && !month.trim().isEmpty()) {
            String trimmed = month.trim();
            if (trimmed.contains("-")) {
                String[] parts = trimmed.split("-");
                if (parts.length >= 2) {
                    try {
                        filterYear = Integer.parseInt(parts[0]);
                        filterMonth = Integer.parseInt(parts[1]);
                    } catch (NumberFormatException e) { }
                }
            } else {
                try {
                    filterMonth = Integer.parseInt(trimmed);
                } catch (NumberFormatException e) { }
            }
        }

        return appointmentRepository.findForParentWithFilters(studentIds, studentId, statusEnum, filterMonth, filterYear, pageable)
                .map(this::convertToDto);
    }

    private void sendStateChangeEmailToParent(Appointment appointment, String statusMsg, String emailSubject) {
        try {
            StudentProfile student = studentProfileRepository.findById(appointment.getStudentId()).orElse(null);
            if (student == null) return;

            User parentUser = userRepository.findById(student.getParentId()).orElse(null);
            if (parentUser == null) return;

            User tutorUser = userRepository.findById(appointment.getTutorId()).orElse(null);
            String tutorName = tutorUser != null ? tutorUser.getFullName() : "Gia sư";

            String content = "<h3>Cập nhật trạng thái lịch học - SmartEd</h3>" +
                    "<p>Xin chào phụ huynh <strong>" + parentUser.getFullName() + "</strong>,</p>" +
                    "<p>Lớp học của học sinh <strong>" + student.getFullName() + "</strong> " + statusMsg + ":</p>" +
                    "<ul>" +
                    "<li><strong>Gia sư:</strong> " + tutorName + "</li>" +
                    "<li><strong>Thời gian:</strong> " + appointment.getStartTime().format(DATE_TIME_FORMATTER) + " - " + appointment.getEndTime().format(DATE_TIME_FORMATTER) + "</li>" +
                    "<li><strong>Trạng thái:</strong> " + appointment.getStatus() + "</li>" +
                    "</ul>" +
                    "<p>Cảm ơn bạn đã tin tưởng dịch vụ gia sư SmartEd!</p>";

            emailService.sendEmail(parentUser.getEmail(), emailSubject, content);
        } catch (Exception e) {
            System.err.println("Lỗi gửi email cập nhật trạng thái: " + e.getMessage());
        }
    }

    private AppointmentResponse convertToDto(Appointment a) {
        StudentProfile student = studentProfileRepository.findById(a.getStudentId()).orElse(null);
        String studentName = student != null ? student.getFullName() : "Học sinh";

        User tutorUser = userRepository.findById(a.getTutorId()).orElse(null);
        String tutorName = tutorUser != null ? tutorUser.getFullName() : "Gia sư";
        String tutorAvatar = tutorUser != null ? tutorUser.getAvatarUrl() : null;

        String subjectName = "Môn học";
        if (a.getSubjectId() != null) {
            TutorSubjectId tsId = new TutorSubjectId(a.getTutorId(), a.getSubjectId());
            TutorSubject ts = tutorSubjectRepository.findById(tsId).orElse(null);
            if (ts != null && ts.getSubject() != null) {
                subjectName = ts.getSubject().getName();
            }
        }

        return AppointmentResponse.builder()
                .id(a.getId())
                .studentId(a.getStudentId())
                .studentName(studentName)
                .tutorId(a.getTutorId())
                .tutorName(tutorName)
                .tutorAvatar(tutorAvatar)
                .subjectId(a.getSubjectId())
                .subjectName(subjectName)
                .startTime(a.getStartTime())
                .endTime(a.getEndTime())
                .status(a.getStatus())
                .paymentStatus(a.getPaymentStatus())
                .cancellationReason(a.getCancellationReason())
                .totalPrice(a.getTotalPrice())
                .feedbackId(a.getFeedback() != null ? a.getFeedback().getId() : null)
                .createdAt(a.getCreatedAt())
                .build();
    }

    @Override
    @Transactional
    public void processVNPayCallback(Map<String, String> params) {
        String secureHash = params.get("vnp_SecureHash");
        String txnRef = params.get("vnp_TxnRef");
        String responseCode = params.get("vnp_ResponseCode");

        if (secureHash == null || txnRef == null || responseCode == null) {
            throw new IllegalArgumentException("Tham số callback không hợp lệ");
        }

        // Verify signature
        Map<String, String> fields = new HashMap<>(params);
        fields.remove("vnp_SecureHash");
        fields.remove("vnp_SecureHashType");

        String hashData = VNPayUtil.getPaymentURL(fields, false);
        String calculatedHash = VNPayUtil.hmacSHA512(vnPayConfig.getSecretKey(), hashData);

        if (!calculatedHash.equalsIgnoreCase(secureHash)) {
            throw new IllegalArgumentException("Chữ ký bảo mật không hợp lệ");
        }

        Integer appointmentId = Integer.parseInt(txnRef);
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy lịch học"));

        if ("00".equals(responseCode)) {
            if (appointment.getPaymentStatus() == PaymentStatus.UNPAID) {
                appointment.setPaymentStatus(PaymentStatus.PAID);
                appointmentRepository.save(appointment);

                // Create and save Transaction bill log
                try {
                    Transaction transaction = new Transaction();
                    transaction.setAppointmentId(appointment.getId());
                    transaction.setAmount(appointment.getTotalPrice());
                    transaction.setPaymentMethod("VNPAY");
                    transaction.setStatus(com.smarted.ed.enums.TransactionStatus.SUCCESS);
                    transaction.setTransactionDate(LocalDateTime.now());
                    transactionRepository.save(transaction);
                } catch (Exception e) {
                    System.err.println("Lỗi ghi hóa đơn Transaction: " + e.getMessage());
                }

                // Send email notifications
                sendAppointmentConfirmationEmails(appointment);
            }
        } else {
            throw new IllegalStateException("Thanh toán thất bại với mã lỗi: " + responseCode);
        }
    }

    private void sendAppointmentConfirmationEmails(Appointment appointment) {
        try {
            StudentProfile student = studentProfileRepository.findById(appointment.getStudentId()).orElse(null);
            TutorProfile tutor = tutorProfileRepository.findById(appointment.getTutorId()).orElse(null);
            if (student == null || tutor == null) {
                return;
            }

            TutorSubjectId compositeId = new TutorSubjectId(tutor.getUserId(), appointment.getSubjectId());
            TutorSubject tutorSubject = tutorSubjectRepository.findById(compositeId).orElse(null);
            String subjectName = (tutorSubject != null && tutorSubject.getSubject() != null) ? tutorSubject.getSubject().getName() : "môn học";

            User parentUser = userRepository.findById(student.getParentId()).orElse(null);
            User tutorUser = tutor.getUser();

            if (tutorUser != null) {
                String tutorEmail = tutorUser.getEmail();
                String parentName = parentUser != null ? parentUser.getFullName() : "Phụ huynh";

                String tutorMailContent = "<h3>Yêu cầu đặt lịch học mới - SmartEd</h3>" +
                        "<p>Xin chào gia sư <strong>" + tutorUser.getFullName() + "</strong>,</p>" +
                        "<p>Bạn có một yêu cầu đăng ký lịch học mới từ phụ huynh <strong>" + parentName + "</strong>:</p>" +
                        "<ul>" +
                        "<li><strong>Học sinh:</strong> " + student.getFullName() + " (Lớp " + student.getGrade() + ")</li>" +
                        "<li><strong>Môn học:</strong> " + subjectName + "</li>" +
                        "<li><strong>Thời gian:</strong> " + appointment.getStartTime().format(DATE_TIME_FORMATTER) + " - " + appointment.getEndTime().format(DATE_TIME_FORMATTER) + "</li>" +
                        "<li><strong>Học phí:</strong> " + appointment.getTotalPrice().toString() + " VND</li>" +
                        "</ul>" +
                        "<p>Vui lòng đăng nhập vào hệ thống để duyệt hoặc từ chối lịch học này.</p>";

                emailService.sendEmail(tutorEmail, "Yêu cầu đặt lịch học mới từ phụ huynh - SmartEd", tutorMailContent);
            }

            if (parentUser != null) {
                String parentEmail = parentUser.getEmail();

                String parentMailContent = "<h3>Đặt lịch học thành công - SmartEd</h3>" +
                        "<p>Xin chào phụ huynh <strong>" + parentUser.getFullName() + "</strong>,</p>" +
                        "<p>Yêu cầu đặt lịch học cho học sinh <strong>" + student.getFullName() + "</strong> đã được gửi thành công:</p>" +
                        "<ul>" +
                        "<li><strong>Gia sư:</strong> " + (tutorUser != null ? tutorUser.getFullName() : "Gia sư") + "</li>" +
                        "<li><strong>Môn học:</strong> " + subjectName + "</li>" +
                        "<li><strong>Thời gian:</strong> " + appointment.getStartTime().format(DATE_TIME_FORMATTER) + " - " + appointment.getEndTime().format(DATE_TIME_FORMATTER) + "</li>" +
                        "<li><strong>Tổng tiền (đã thanh toán):</strong> " + appointment.getTotalPrice().toString() + " VND</li>" +
                        "</ul>" +
                        "<p>Lịch học hiện đang ở trạng thái <strong>Chờ gia sư xác nhận</strong>. Chúng tôi sẽ thông báo cho bạn khi gia sư phản hồi.</p>";

                emailService.sendEmail(parentEmail, "Đặt lịch học thành công và chờ gia sư duyệt - SmartEd", parentMailContent);
            }
        } catch (Exception e) {
            System.err.println("Lỗi gửi email xác nhận thanh toán: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public AppointmentResponse cancelAppointment(Integer parentId, Integer appointmentId, String reason) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy lịch học"));

        StudentProfile student = studentProfileRepository.findById(appointment.getStudentId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy hồ sơ học sinh"));

        if (!student.getParentId().equals(parentId)) {
            throw new IllegalArgumentException("Bạn không có quyền hủy lịch học này");
        }

        if (appointment.getStatus() != AppointmentStatus.PENDING && appointment.getStatus() != AppointmentStatus.CONFIRMED) {
            throw new IllegalArgumentException("Không thể hủy lịch học ở trạng thái hiện tại");
        }

        appointment.setStatus(AppointmentStatus.CANCELLED);
        appointment.setCancellationReason(reason);
        Appointment saved = appointmentRepository.save(appointment);

        // Send email to tutor
        String statusText = "đã bị hủy bởi phụ huynh với lý do: " + (reason != null ? reason : "Không có lý do cụ thể");
        sendStateChangeEmailToTutor(saved, statusText, "Lịch học đã bị hủy bởi phụ huynh - SmartEd");

        return convertToDto(saved);
    }

    @Override
    public List<AppointmentResponse> getCancelledOrRejectedPaidAppointments() {
        return appointmentRepository.findByStatusInAndPaymentStatus(
                List.of(AppointmentStatus.CANCELLED, AppointmentStatus.REJECTED),
                PaymentStatus.PAID
        ).stream().map(this::convertToDto).collect(Collectors.toList());
    }

    private void sendStateChangeEmailToTutor(Appointment appointment, String statusMsg, String emailSubject) {
        try {
            User tutorUser = userRepository.findById(appointment.getTutorId()).orElse(null);
            if (tutorUser == null) return;

            StudentProfile student = studentProfileRepository.findById(appointment.getStudentId()).orElse(null);
            String studentName = student != null ? student.getFullName() : "Học sinh";

            String content = "<h3>Cập nhật trạng thái lịch học - SmartEd</h3>" +
                    "<p>Xin chào gia sư <strong>" + tutorUser.getFullName() + "</strong>,</p>" +
                    "<p>Lớp học của học sinh <strong>" + studentName + "</strong> " + statusMsg + ":</p>" +
                    "<ul>" +
                    "<li><strong>Thời gian:</strong> " + appointment.getStartTime().format(DATE_TIME_FORMATTER) + " - " + appointment.getEndTime().format(DATE_TIME_FORMATTER) + "</li>" +
                    "<li><strong>Trạng thái:</strong> " + appointment.getStatus() + "</li>" +
                    "</ul>" +
                    "<p>Trân trọng,<br/>Đội ngũ SmartEd</p>";

            emailService.sendEmail(tutorUser.getEmail(), emailSubject, content);
        } catch (Exception e) {
            System.err.println("Lỗi gửi email cập nhật trạng thái tới gia sư: " + e.getMessage());
        }
    }
}
