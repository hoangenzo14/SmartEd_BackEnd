package com.smarted.ed.service;

import com.smarted.ed.dto.SubjectRequest;
import com.smarted.ed.entity.Subject;

import java.util.List;

public interface SubjectService {
    List<Subject> getAllSubjects(String search);
    Subject getSubjectById(Integer id);
    Subject createSubject(SubjectRequest request);
    Subject updateSubject(Integer id, SubjectRequest request);
    void deleteSubject(Integer id);
}
