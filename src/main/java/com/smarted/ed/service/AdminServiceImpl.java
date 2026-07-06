package com.smarted.ed.service;

import com.smarted.ed.dto.AdminTutorResponse;
import com.smarted.ed.dto.TutorSubjectDto;
import com.smarted.ed.entity.TutorProfile;
import com.smarted.ed.entity.TutorSubject;
import com.smarted.ed.entity.TutorSubjectId;
import com.smarted.ed.enums.ApprovalStatus;
import com.smarted.ed.exception.ResourceNotFoundException;
import jakarta.persistence.EntityNotFoundException;
import com.smarted.ed.repository.TutorProfileRepository;
import com.smarted.ed.repository.TutorSubjectRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.math.BigDecimal;

@Service
public class AdminServiceImpl implements AdminService {

    @Autowired
    private TutorProfileRepository tutorProfileRepository;

    @Autowired
    private TutorSubjectRepository tutorSubjectRepository;

    @Override
    public Page<AdminTutorResponse> getAllTutors(ApprovalStatus status, String search, int page, int size, String sortBy, String sortDir) {
        Pageable pageable = PageRequest.of(page, size);
        Page<TutorProfile> tutorsPage = tutorProfileRepository.findAllTutorsForAdmin(status, search, sortBy, sortDir, pageable);

        return tutorsPage.map(this::convertToDto);
    }

    @Override
    public AdminTutorResponse getTutorById(Integer id) {
        TutorProfile tutor = tutorProfileRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy gia sư có ID: " + id));
        return convertToDto(tutor);
    }

    @Override
    @Transactional
    public void approveTutor(Integer id) {
        TutorProfile tutor = tutorProfileRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy gia sư có ID: " + id));
        tutor.setApprovalStatus(ApprovalStatus.APPROVED);
        tutor.setRejectReason(null);
        tutorProfileRepository.save(tutor);
    }

    @Override
    @Transactional
    public void rejectTutor(Integer id, String reason) {
        TutorProfile tutor = tutorProfileRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy gia sư có ID: " + id));
        tutor.setApprovalStatus(ApprovalStatus.REJECTED);
        tutor.setRejectReason(reason);
        tutorProfileRepository.save(tutor);
    }

    @Override
    @Transactional
    public void approveTutorSubject(Integer tutorId, Integer subjectId) {
        TutorSubjectId compositeId = new TutorSubjectId(tutorId, subjectId);
        TutorSubject tutorSubject = tutorSubjectRepository.findById(compositeId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy liên kết môn học của gia sư"));
        tutorSubject.setStatus(ApprovalStatus.APPROVED);
        tutorSubject.setRejectReason(null);
        tutorSubjectRepository.save(tutorSubject);

        // Update parent TutorProfile status
        TutorProfile tutor = tutorSubject.getTutor();
        updateTutorProfileApprovalStatus(tutor);
        tutorProfileRepository.save(tutor);
    }

    @Override
    @Transactional
    public void rejectTutorSubject(Integer tutorId, Integer subjectId, String reason) {
        TutorSubjectId compositeId = new TutorSubjectId(tutorId, subjectId);
        TutorSubject tutorSubject = tutorSubjectRepository.findById(compositeId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy liên kết môn học của gia sư"));
        tutorSubject.setStatus(ApprovalStatus.REJECTED);
        tutorSubject.setRejectReason(reason);
        tutorSubjectRepository.save(tutorSubject);

        // Update parent TutorProfile status
        TutorProfile tutor = tutorSubject.getTutor();
        updateTutorProfileApprovalStatus(tutor);
        tutorProfileRepository.save(tutor);
    }

    private void updateTutorProfileApprovalStatus(TutorProfile tutor) {
        List<TutorSubject> subjects = tutor.getTutorSubjects();
        if (subjects == null || subjects.isEmpty()) {
            tutor.setApprovalStatus(ApprovalStatus.PENDING);
            tutor.setRejectReason(null);
            return;
        }

        boolean hasApproved = subjects.stream()
                .anyMatch(s -> s.getStatus() == ApprovalStatus.APPROVED);

        if (hasApproved) {
            tutor.setApprovalStatus(ApprovalStatus.APPROVED);
            tutor.setRejectReason(null);
            return;
        }

        boolean allRejected = subjects.stream()
                .allMatch(s -> s.getStatus() == ApprovalStatus.REJECTED);

        if (allRejected) {
            tutor.setApprovalStatus(ApprovalStatus.REJECTED);
            tutor.setRejectReason("Tất cả môn học đã đăng ký đều bị từ chối");
        } else {
            tutor.setApprovalStatus(ApprovalStatus.PENDING);
            tutor.setRejectReason(null);
        }
    }

    private AdminTutorResponse convertToDto(TutorProfile tutorProfile) {
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

        String defaultBio = subjects.isEmpty() ? null : subjects.get(0).getBio();
        BigDecimal defaultHourlyRate = subjects.isEmpty() ? null : subjects.get(0).getHourlyRate();

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
                .build();
    }
}
