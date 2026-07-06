package com.smarted.ed.service;

import com.smarted.ed.dto.FeedbackRequest;
import com.smarted.ed.dto.FeedbackResponse;
import com.smarted.ed.entity.Appointment;
import com.smarted.ed.entity.Feedback;
import com.smarted.ed.entity.StudentProfile;
import com.smarted.ed.entity.TutorProfile;
import com.smarted.ed.entity.User;
import com.smarted.ed.enums.AppointmentStatus;
import com.smarted.ed.exception.BadRequestException;
import com.smarted.ed.exception.ResourceNotFoundException;
import com.smarted.ed.repository.AppointmentRepository;
import com.smarted.ed.repository.FeedbackRepository;
import com.smarted.ed.repository.StudentProfileRepository;
import com.smarted.ed.repository.TutorProfileRepository;
import com.smarted.ed.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class FeedbackServiceImpl implements FeedbackService {

    @Autowired
    private FeedbackRepository feedbackRepository;

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private StudentProfileRepository studentProfileRepository;

    @Autowired
    private TutorProfileRepository tutorProfileRepository;

    @Autowired
    private UserRepository userRepository;

    @Override
    @Transactional
    public FeedbackResponse createFeedback(Integer parentId, FeedbackRequest request) {
        Appointment appointment = appointmentRepository.findById(request.getAppointmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy lịch học"));

        // Validate that student belongs to the parent
        StudentProfile student = studentProfileRepository.findById(appointment.getStudentId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy hồ sơ học sinh"));

        if (!student.getParentId().equals(parentId)) {
            throw new BadRequestException("Lịch học này không thuộc quyền quản lý của bạn");
        }

        // Validate appointment is completed
        if (appointment.getStatus() != AppointmentStatus.COMPLETED) {
            throw new BadRequestException("Chỉ có thể đánh giá lịch học đã hoàn thành");
        }

        // Validate that feedback does not already exist
        if (feedbackRepository.findByAppointmentId(appointment.getId()).isPresent()) {
            throw new BadRequestException("Lịch học này đã được đánh giá trước đó");
        }

        // Create feedback
        Feedback feedback = new Feedback();
        feedback.setAppointmentId(appointment.getId());
        feedback.setParentId(parentId);
        feedback.setTutorId(appointment.getTutorId());
        feedback.setRating(request.getRating());
        feedback.setComment(request.getComment());
        
        Feedback saved = feedbackRepository.save(feedback);

        // Recalculate tutor's average rating
        Double avgRating = feedbackRepository.getAverageRatingForTutor(appointment.getTutorId());
        TutorProfile tutorProfile = tutorProfileRepository.findById(appointment.getTutorId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy hồ sơ gia sư"));
        
        BigDecimal bdAvg = BigDecimal.valueOf(avgRating != null ? avgRating : 0.0)
                .setScale(2, RoundingMode.HALF_UP);
        tutorProfile.setAverageRating(bdAvg);
        tutorProfileRepository.save(tutorProfile);

        return convertToResponse(saved);
    }

    @Override
    public FeedbackResponse getFeedbackByAppointment(Integer appointmentId) {
        Feedback feedback = feedbackRepository.findByAppointmentId(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đánh giá cho lịch học này"));
        return convertToResponse(feedback);
    }

    @Override
    public Page<FeedbackResponse> getFeedbacksByTutor(Integer tutorId, int page, int size) {
        if (!tutorProfileRepository.existsById(tutorId)) {
            throw new ResourceNotFoundException("Không tìm thấy hồ sơ gia sư");
        }
        Page<Feedback> feedbacksPage = feedbackRepository.findByTutorId(tutorId, PageRequest.of(page, size));
        return feedbacksPage.map(this::convertToResponse);
    }

    private FeedbackResponse convertToResponse(Feedback f) {
        User parent = userRepository.findById(f.getParentId()).orElse(null);
        String parentName = parent != null ? parent.getFullName() : "Phụ huynh";

        User tutorUser = userRepository.findById(f.getTutorId()).orElse(null);
        String tutorName = tutorUser != null ? tutorUser.getFullName() : "Gia sư";

        return FeedbackResponse.builder()
                .id(f.getId())
                .appointmentId(f.getAppointmentId())
                .parentId(f.getParentId())
                .parentName(parentName)
                .tutorId(f.getTutorId())
                .tutorName(tutorName)
                .rating(f.getRating())
                .comment(f.getComment())
                .createdAt(f.getCreatedAt())
                .build();
    }
}
