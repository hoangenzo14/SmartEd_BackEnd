package com.smarted.ed.service;

import com.smarted.ed.dto.AdminTutorResponse;
import com.smarted.ed.enums.ApprovalStatus;
import org.springframework.data.domain.Page;

public interface AdminService {
    Page<AdminTutorResponse> getAllTutors(ApprovalStatus status, String search, int page, int size, String sortBy, String sortDir);
    AdminTutorResponse getTutorById(Integer id);
    void approveTutor(Integer id);
    void rejectTutor(Integer id, String reason);
    void approveTutorSubject(Integer tutorId, Integer subjectId);
    void rejectTutorSubject(Integer tutorId, Integer subjectId, String reason);
}
