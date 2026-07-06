package com.smarted.ed.controller;

import com.smarted.ed.configs.CustomUserDetails;
import com.smarted.ed.dto.ApiResponse;
import com.smarted.ed.dto.FeedbackRequest;
import com.smarted.ed.dto.FeedbackResponse;
import com.smarted.ed.entity.User;
import com.smarted.ed.repository.UserRepository;
import com.smarted.ed.service.FeedbackService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/feedbacks")
public class FeedbackController {

    @Autowired
    private FeedbackService feedbackService;

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
    public ResponseEntity<ApiResponse<FeedbackResponse>> createFeedback(@Valid @RequestBody FeedbackRequest request) {
        FeedbackResponse response = feedbackService.createFeedback(getLoggedInUserId(), request);
        return ResponseEntity.ok(ApiResponse.success("Đánh giá gia sư thành công", response));
    }

    @GetMapping("/appointment/{appointmentId}")
    public ResponseEntity<ApiResponse<FeedbackResponse>> getFeedbackByAppointment(@PathVariable("appointmentId") Integer appointmentId) {
        FeedbackResponse response = feedbackService.getFeedbackByAppointment(appointmentId);
        return ResponseEntity.ok(ApiResponse.success("Lấy thông tin đánh giá thành công", response));
    }

    @GetMapping("/tutor/{tutorId}")
    public ResponseEntity<ApiResponse<org.springframework.data.domain.Page<FeedbackResponse>>> getFeedbacksByTutor(
            @PathVariable("tutorId") Integer tutorId,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "3") int size
    ) {
        org.springframework.data.domain.Page<FeedbackResponse> response = feedbackService.getFeedbacksByTutor(tutorId, page, size);
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách đánh giá gia sư thành công", response));
    }
}
