package com.smarted.ed.controller;

import com.smarted.ed.configs.CustomUserDetails;
import com.smarted.ed.dto.ApiResponse;
import com.smarted.ed.dto.AppointmentRequest;
import com.smarted.ed.dto.AppointmentResponse;
import com.smarted.ed.dto.RejectRequest;
import com.smarted.ed.entity.User;
import com.smarted.ed.repository.UserRepository;
import com.smarted.ed.service.AppointmentService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/appointments")
public class AppointmentController {

    @Autowired
    private AppointmentService appointmentService;

    @Autowired
    private UserRepository userRepository;

    private Integer getLoggedInUserId() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof CustomUserDetails) {
            return ((CustomUserDetails) principal).getId();
        }
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .map(User::getId)
                .orElseThrow(() -> new IllegalStateException("Người dùng chưa được xác thực hoặc thông tin xác thực không hợp lệ"));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<AppointmentResponse>> createAppointment(@Valid @RequestBody AppointmentRequest request) {
        AppointmentResponse res = appointmentService.createAppointment(getLoggedInUserId(), request);
        return ResponseEntity.ok(ApiResponse.success("Đặt lịch học thành công", res));
    }

    @GetMapping("/parent")
    public ResponseEntity<ApiResponse<org.springframework.data.domain.Page<AppointmentResponse>>> getParentAppointments(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size,
            @RequestParam(name = "studentId", required = false) Integer studentId,
            @RequestParam(name = "status", required = false) String status,
            @RequestParam(name = "month", required = false) String month) {
        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(page, size, org.springframework.data.domain.Sort.by("startTime").descending());
        org.springframework.data.domain.Page<AppointmentResponse> res = appointmentService.getAppointmentsForParent(getLoggedInUserId(), studentId, status, month, pageable);
        return ResponseEntity.ok(ApiResponse.success("Lấy lịch sử đặt lớp thành công", res));
    }

    @GetMapping("/tutor")
    public ResponseEntity<ApiResponse<org.springframework.data.domain.Page<AppointmentResponse>>> getTutorAppointments(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size,
            @RequestParam(name = "status", required = false) String status,
            @RequestParam(name = "month", required = false) String month) {
        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(page, size, org.springframework.data.domain.Sort.by("startTime").descending());
        org.springframework.data.domain.Page<AppointmentResponse> res = appointmentService.getAppointmentsForTutor(getLoggedInUserId(), status, month, pageable);
        return ResponseEntity.ok(ApiResponse.success("Lấy lịch dạy thành công", res));
    }

    @PutMapping("/{id}/accept")
    public ResponseEntity<ApiResponse<AppointmentResponse>> acceptAppointment(@PathVariable("id") Integer appointmentId) {
        AppointmentResponse res = appointmentService.acceptAppointment(getLoggedInUserId(), appointmentId);
        return ResponseEntity.ok(ApiResponse.success("Duyệt lịch học thành công", res));
    }

    @PutMapping("/{id}/reject")
    public ResponseEntity<ApiResponse<AppointmentResponse>> rejectAppointment(
            @PathVariable("id") Integer appointmentId,
            @Valid @RequestBody RejectRequest request
    ) {
        AppointmentResponse res = appointmentService.rejectAppointment(getLoggedInUserId(), appointmentId, request.getReason());
        return ResponseEntity.ok(ApiResponse.success("Từ chối lịch học thành công", res));
    }

    @PutMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<AppointmentResponse>> cancelAppointment(
            @PathVariable("id") Integer appointmentId,
            @Valid @RequestBody RejectRequest request
    ) {
        AppointmentResponse res = appointmentService.cancelAppointment(getLoggedInUserId(), appointmentId, request.getReason());
        return ResponseEntity.ok(ApiResponse.success("Hủy lịch học thành công", res));
    }

    @PutMapping("/{id}/complete")
    public ResponseEntity<ApiResponse<AppointmentResponse>> completeAppointment(@PathVariable("id") Integer appointmentId) {
        AppointmentResponse res = appointmentService.completeAppointment(getLoggedInUserId(), appointmentId);
        return ResponseEntity.ok(ApiResponse.success("Hoàn thành buổi học thành công", res));
    }

    @GetMapping("/vnpay-callback")
    public ResponseEntity<ApiResponse<Void>> handleVNPayCallback(
            @RequestParam java.util.Map<String, String> params
    ) {
        appointmentService.processVNPayCallback(params);
        return ResponseEntity.ok(ApiResponse.success("Thanh toán thành công"));
    }
}
