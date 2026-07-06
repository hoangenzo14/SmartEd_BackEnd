package com.smarted.ed.service;

import com.smarted.ed.dto.AdminTutorResponse;
import com.smarted.ed.dto.TutorProfileDto;
import com.smarted.ed.dto.TutorProfileUpdateRequest;
import com.smarted.ed.dto.TutorSubjectDto;
import com.smarted.ed.dto.TutorSubjectRegisterRequest;
import org.springframework.data.domain.Page;
import java.util.List;

public interface TutorService {
    Page<TutorProfileDto> getApprovedTutors(String search, int page, int size, String sortBy, String sortDir);
    List<TutorSubjectDto> getMySubjects(Integer tutorId);
    void registerSubject(Integer tutorId, TutorSubjectRegisterRequest request);
    Page<TutorProfileDto> searchTutors(String search, Integer subjectId, java.math.BigDecimal minPrice, java.math.BigDecimal maxPrice, java.math.BigDecimal minRating, int page, int size, String sortBy, String sortDir);
    AdminTutorResponse getTutorDetails(Integer id);
    TutorProfileDto updateProfile(Integer tutorId, TutorProfileUpdateRequest request);
}
