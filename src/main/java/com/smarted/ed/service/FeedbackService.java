package com.smarted.ed.service;

import com.smarted.ed.dto.FeedbackRequest;
import com.smarted.ed.dto.FeedbackResponse;

public interface FeedbackService {
    FeedbackResponse createFeedback(Integer parentId, FeedbackRequest request);
    FeedbackResponse getFeedbackByAppointment(Integer appointmentId);
    org.springframework.data.domain.Page<FeedbackResponse> getFeedbacksByTutor(Integer tutorId, int page, int size);
}
