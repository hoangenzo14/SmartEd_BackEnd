package com.smarted.ed.service;

import com.smarted.ed.dto.AdminTutorResponse;
import com.smarted.ed.dto.TutorProfileDto;
import com.smarted.ed.dto.TutorProfileUpdateRequest;
import com.smarted.ed.dto.TutorSubjectDto;
import com.smarted.ed.dto.TutorSubjectRegisterRequest;
import com.smarted.ed.dto.WeeklyScheduleSlotDto;
import com.smarted.ed.entity.Appointment;
import com.smarted.ed.entity.Subject;
import com.smarted.ed.entity.TutorProfile;
import com.smarted.ed.entity.TutorSubject;
import com.smarted.ed.entity.TutorSubjectId;
import com.smarted.ed.entity.User;
import com.smarted.ed.enums.ApprovalStatus;
import com.smarted.ed.enums.AppointmentStatus;
import com.smarted.ed.exception.ResourceNotFoundException;
import com.smarted.ed.repository.AppointmentRepository;
import com.smarted.ed.repository.SubjectRepository;
import com.smarted.ed.repository.TutorProfileRepository;
import com.smarted.ed.repository.TutorSubjectRepository;
import com.smarted.ed.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.math.BigDecimal;

@Service
public class TutorServiceImpl implements TutorService {

    @Autowired
    private TutorProfileRepository tutorProfileRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SubjectRepository subjectRepository;

    @Autowired
    private TutorSubjectRepository tutorSubjectRepository;

    @Autowired
    private CloudinaryService cloudinaryService;

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Override
    public Page<TutorProfileDto> getApprovedTutors(String search, int page, int size, String sortBy, String sortDir) {
        Pageable pageable = PageRequest.of(page, size);
        Page<TutorProfile> tutorsPage = tutorProfileRepository.findByApprovalStatusAndSearch(
                ApprovalStatus.APPROVED, search, sortBy, sortDir, pageable);

        return tutorsPage.map(this::convertToDto);
    }

    private TutorProfileDto convertToDto(TutorProfile tutorProfile) {
        List<TutorSubject> approvedSubjects = tutorProfile.getTutorSubjects() == null ? List.of() :
                tutorProfile.getTutorSubjects().stream()
                        .filter(ts -> ts.getStatus() == ApprovalStatus.APPROVED)
                        .toList();

        List<String> subjects = approvedSubjects.stream()
                .map(ts -> ts.getSubject().getName())
                .toList();

        String defaultBio = approvedSubjects.isEmpty() ? null : approvedSubjects.get(0).getBio();
        BigDecimal defaultHourlyRate = approvedSubjects.isEmpty() ? null : approvedSubjects.get(0).getHourlyRate();

        return TutorProfileDto.builder()
                .userId(tutorProfile.getUserId())
                .fullName(tutorProfile.getUser() != null ? tutorProfile.getUser().getFullName() : null)
                .email(tutorProfile.getUser() != null ? tutorProfile.getUser().getEmail() : null)
                .phone(tutorProfile.getUser() != null ? tutorProfile.getUser().getPhone() : null)
                .avatarUrl(tutorProfile.getUser() != null ? tutorProfile.getUser().getAvatarUrl() : null)
                .bio(defaultBio)
                .location(tutorProfile.getLocation())
                .hourlyRate(defaultHourlyRate)
                .averageRating(tutorProfile.getAverageRating())
                .subjects(subjects)
                .build();
    }

    @Override
    public List<TutorSubjectDto> getMySubjects(Integer tutorId) {
        TutorProfile tutor = tutorProfileRepository.findById(tutorId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy hồ sơ gia sư"));

        List<TutorSubject> tutorSubjects = tutorSubjectRepository.findByTutorUserId(tutor.getUserId());
        return tutorSubjects.stream()
                .map(ts -> TutorSubjectDto.builder()
                        .subjectId(ts.getSubject().getId())
                        .subjectName(ts.getSubject().getName())
                        .status(ts.getStatus())
                        .certificateUrl(ts.getCertificateUrl())
                        .rejectReason(ts.getRejectReason())
                        .bio(ts.getBio())
                        .hourlyRate(ts.getHourlyRate())
                        .build())
                .toList();
    }

    @Override
    @Transactional
    public void registerSubject(Integer tutorId, TutorSubjectRegisterRequest request) {
        TutorProfile tutor = tutorProfileRepository.findById(tutorId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy hồ sơ gia sư"));

        Subject subject = subjectRepository.findById(request.getSubjectId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy môn học có ID: " + request.getSubjectId()));

        if (!Boolean.TRUE.equals(subject.getIsActive())) {
            throw new IllegalArgumentException("Môn học này hiện không hoạt động");
        }

        TutorSubjectId compositeId = new TutorSubjectId(tutor.getUserId(), subject.getId());
        if (tutorSubjectRepository.existsById(compositeId)) {
            throw new IllegalArgumentException("Môn học này đã được đăng ký trước đó");
        }

        TutorSubject ts = new TutorSubject();
        ts.setId(compositeId);
        ts.setTutor(tutor);
        ts.setSubject(subject);
        ts.setStatus(ApprovalStatus.PENDING);
        ts.setBio(request.getBio());
        ts.setHourlyRate(request.getHourlyRate());
        ts.setCertificateUrl(request.getCertificateUrl());

        tutorSubjectRepository.save(ts);
    }

    @Override
    public Page<TutorProfileDto> searchTutors(String search, Integer subjectId, java.math.BigDecimal minPrice, java.math.BigDecimal maxPrice, java.math.BigDecimal minRating, int page, int size, String sortBy, String sortDir) {
        Pageable pageable = PageRequest.of(page, size);
        Page<TutorProfile> tutorsPage = tutorProfileRepository.searchTutors(
                search, subjectId, minPrice, maxPrice, minRating, sortBy, sortDir, pageable);
        return tutorsPage.map(this::convertToDto);
    }

    @Override
    public AdminTutorResponse getTutorDetails(Integer id) {
        TutorProfile tutorProfile = tutorProfileRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy gia sư có ID: " + id));

        List<TutorSubjectDto> subjects = tutorProfile.getTutorSubjects() == null ? List.of() :
                tutorProfile.getTutorSubjects().stream()
                        .map(ts -> TutorSubjectDto.builder()
                                .subjectId(ts.getSubject() != null ? ts.getSubject().getId() : null)
                                .subjectName(ts.getSubject() != null ? ts.getSubject().getName() : null)
                                .status(ts.getStatus())
                                .certificateUrl(ts.getCertificateUrl())
                                .rejectReason(ts.getRejectReason())
                                .bio(ts.getBio())
                                .hourlyRate(ts.getHourlyRate())
                                .build())
                        .toList();

        // Get default bio and rate from approved subjects if available
        List<TutorSubjectDto> approvedSubjects = subjects.stream()
                .filter(s -> s.getStatus() == ApprovalStatus.APPROVED)
                .toList();

        String defaultBio = approvedSubjects.isEmpty() ?
                (subjects.isEmpty() ? null : subjects.get(0).getBio()) :
                approvedSubjects.get(0).getBio();

        BigDecimal defaultHourlyRate = approvedSubjects.isEmpty() ?
                (subjects.isEmpty() ? null : subjects.get(0).getHourlyRate()) :
                approvedSubjects.get(0).getHourlyRate();

        List<Appointment> tutorAppointments = appointmentRepository.findByTutorId(id);
        List<WeeklyScheduleSlotDto> bookedSlots = tutorAppointments.stream()
                .filter(app -> app.getStatus() == AppointmentStatus.CONFIRMED || app.getStatus() == AppointmentStatus.COMPLETED)
                .map(app -> {
                    String day = app.getStartTime().getDayOfWeek().name();
                    day = day.substring(0, 1).toUpperCase() + day.substring(1).toLowerCase();
                    String slotTime = app.getStartTime().toLocalTime().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"));
                    return WeeklyScheduleSlotDto.builder()
                            .dayOfWeek(day)
                            .slotTime(slotTime)
                            .status("BOOKED")
                            .build();
                })
                .distinct()
                .toList();

        return AdminTutorResponse.builder()
                .userId(tutorProfile.getUserId())
                .fullName(tutorProfile.getUser() != null ? tutorProfile.getUser().getFullName() : null)
                .email(tutorProfile.getUser() != null ? tutorProfile.getUser().getEmail() : null)
                .phone(tutorProfile.getUser() != null ? tutorProfile.getUser().getPhone() : null)
                .avatarUrl(tutorProfile.getUser() != null ? tutorProfile.getUser().getAvatarUrl() : null)
                .bio(defaultBio)
                .location(tutorProfile.getLocation())
                .hourlyRate(defaultHourlyRate)
                .averageRating(tutorProfile.getAverageRating())
                .approvalStatus(tutorProfile.getApprovalStatus())
                .rejectReason(tutorProfile.getRejectReason())
                .subjects(subjects)
                .bookedSlots(bookedSlots)
                .build();
    }

    @Override
    @Transactional
    public TutorProfileDto updateProfile(Integer tutorId, TutorProfileUpdateRequest request) {
        TutorProfile tutorProfile = tutorProfileRepository.findById(tutorId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy hồ sơ gia sư"));

        User user = tutorProfile.getUser();
        if (user == null) {
            user = userRepository.findById(tutorId)
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tài khoản người dùng"));
        }

        // Update User fields
        if (request.getPhone() != null) {
            user.setPhone(request.getPhone());
        }
        if (request.getFile() != null && !request.getFile().isEmpty()) {
            try {
                String uploadedUrl = cloudinaryService.uploadFile(request.getFile());
                user.setAvatarUrl(uploadedUrl);
            } catch (IOException e) {
                throw new RuntimeException("Lỗi khi tải ảnh lên Cloudinary: " + e.getMessage());
            }
        } else if (request.getAvatarUrl() != null) {
            user.setAvatarUrl(request.getAvatarUrl());
        }
        userRepository.save(user);

        // Update TutorProfile fields
        if (request.getLocation() != null) {
            tutorProfile.setLocation(request.getLocation());
        }
        tutorProfileRepository.save(tutorProfile);

        // Refresh and return updated DTO
        TutorProfile refreshed = tutorProfileRepository.findById(tutorId).orElse(tutorProfile);
        return convertToDto(refreshed);
    }
}
