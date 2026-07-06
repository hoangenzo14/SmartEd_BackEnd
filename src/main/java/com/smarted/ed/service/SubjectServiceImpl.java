package com.smarted.ed.service;

import com.smarted.ed.dto.SubjectRequest;
import com.smarted.ed.entity.Subject;
import com.smarted.ed.exception.ResourceNotFoundException;
import com.smarted.ed.repository.SubjectRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
public class SubjectServiceImpl implements SubjectService {

    @Autowired
    private SubjectRepository subjectRepository;

    @Override
    public List<Subject> getAllSubjects(String search) {
        if (StringUtils.hasText(search)) {
            return subjectRepository.findByNameContainingIgnoreCase(search);
        }
        return subjectRepository.findAll();
    }

    @Override
    public Subject getSubjectById(Integer id) {
        return subjectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy môn học có ID: " + id));
    }

    @Override
    @Transactional
    public Subject createSubject(SubjectRequest request) {
        // Validate name unique
        List<Subject> existing = subjectRepository.findByNameContainingIgnoreCase(request.getName());
        for (Subject s : existing) {
            if (s.getName().equalsIgnoreCase(request.getName())) {
                throw new IllegalArgumentException("Tên môn học '" + request.getName() + "' đã tồn tại");
            }
        }

        Subject subject = new Subject();
        subject.setName(request.getName());
        subject.setDescription(request.getDescription());
        subject.setIsActive(request.getIsActive() != null ? request.getIsActive() : true);

        return subjectRepository.save(subject);
    }

    @Override
    @Transactional
    public Subject updateSubject(Integer id, SubjectRequest request) {
        Subject subject = getSubjectById(id);

        // Validate name unique if changed
        if (!subject.getName().equalsIgnoreCase(request.getName())) {
            List<Subject> existing = subjectRepository.findByNameContainingIgnoreCase(request.getName());
            for (Subject s : existing) {
                if (s.getName().equalsIgnoreCase(request.getName()) && !s.getId().equals(id)) {
                    throw new IllegalArgumentException("Tên môn học '" + request.getName() + "' đã tồn tại");
                }
            }
        }

        subject.setName(request.getName());
        subject.setDescription(request.getDescription());
        if (request.getIsActive() != null) {
            subject.setIsActive(request.getIsActive());
        }

        return subjectRepository.save(subject);
    }

    @Override
    @Transactional
    public void deleteSubject(Integer id) {
        Subject subject = getSubjectById(id);

        boolean hasTutors = subject.getTutorSubjects() != null && !subject.getTutorSubjects().isEmpty();
        boolean hasAppointments = subject.getAppointments() != null && !subject.getAppointments().isEmpty();

        if (hasTutors || hasAppointments) {
            // Soft delete
            subject.setIsActive(false);
            subjectRepository.save(subject);
        } else {
            // Hard delete
            subjectRepository.delete(subject);
        }
    }
}
