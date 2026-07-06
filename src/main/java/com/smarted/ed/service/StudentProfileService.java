package com.smarted.ed.service;

import com.smarted.ed.dto.StudentProfileRequest;
import com.smarted.ed.entity.StudentProfile;

import java.util.List;

public interface StudentProfileService {
    List<StudentProfile> getStudentsByParent(Integer parentId);
    StudentProfile createStudentProfile(Integer parentId, StudentProfileRequest request);
    StudentProfile updateStudentProfile(Integer parentId, Integer id, StudentProfileRequest request);
    void deleteStudentProfile(Integer parentId, Integer id);
}
