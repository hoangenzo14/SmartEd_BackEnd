package com.smarted.ed.service;

import com.smarted.ed.dto.StudentProfileRequest;
import com.smarted.ed.entity.StudentProfile;
import com.smarted.ed.exception.ResourceNotFoundException;
import com.smarted.ed.repository.StudentProfileRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.List;

@Service
public class StudentProfileServiceImpl implements StudentProfileService {

    @Autowired
    private StudentProfileRepository studentProfileRepository;

    @Autowired
    private CloudinaryService cloudinaryService;

    @Override
    public List<StudentProfile> getStudentsByParent(Integer parentId) {
        return studentProfileRepository.findByParentId(parentId);
    }

    @Override
    @Transactional
    public StudentProfile createStudentProfile(Integer parentId, StudentProfileRequest request) {
        StudentProfile student = new StudentProfile();
        student.setParentId(parentId);
        student.setFullName(request.getFullName());
        student.setGrade(request.getGrade());
        student.setSchoolName(request.getSchoolName());
        student.setAcademicPerformance(request.getAcademicPerformance());
        student.setNotes(request.getNotes());
        
        if (request.getFile() != null && !request.getFile().isEmpty()) {
            try {
                String uploadedUrl = cloudinaryService.uploadFile(request.getFile());
                student.setAvatarUrl(uploadedUrl);
            } catch (IOException e) {
                throw new RuntimeException("Lỗi khi tải ảnh lên Cloudinary: " + e.getMessage());
            }
        } else {
            student.setAvatarUrl(request.getAvatarUrl());
        }

        return studentProfileRepository.save(student);
    }

    @Override
    @Transactional
    public StudentProfile updateStudentProfile(Integer parentId, Integer id, StudentProfileRequest request) {
        StudentProfile student = studentProfileRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy hồ sơ học sinh có ID: " + id));

        // Security check
        if (!student.getParentId().equals(parentId)) {
            throw new IllegalArgumentException("Bạn không có quyền chỉnh sửa hồ sơ học sinh này.");
        }

        student.setFullName(request.getFullName());
        student.setGrade(request.getGrade());
        student.setSchoolName(request.getSchoolName());
        student.setAcademicPerformance(request.getAcademicPerformance());
        student.setNotes(request.getNotes());
        
        if (request.getFile() != null && !request.getFile().isEmpty()) {
            try {
                String uploadedUrl = cloudinaryService.uploadFile(request.getFile());
                student.setAvatarUrl(uploadedUrl);
            } catch (IOException e) {
                throw new RuntimeException("Lỗi khi tải ảnh lên Cloudinary: " + e.getMessage());
            }
        } else if (request.getAvatarUrl() != null) {
            student.setAvatarUrl(request.getAvatarUrl());
        }

        return studentProfileRepository.save(student);
    }

    @Override
    @Transactional
    public void deleteStudentProfile(Integer parentId, Integer id) {
        StudentProfile student = studentProfileRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy hồ sơ học sinh có ID: " + id));

        // Security check
        if (!student.getParentId().equals(parentId)) {
            throw new IllegalArgumentException("Bạn không có quyền xóa hồ sơ học sinh này.");
        }

        studentProfileRepository.delete(student);
    }
}
