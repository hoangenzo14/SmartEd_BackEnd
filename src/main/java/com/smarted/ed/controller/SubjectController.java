package com.smarted.ed.controller;

import com.smarted.ed.dto.ApiResponse;
import com.smarted.ed.entity.Subject;
import com.smarted.ed.service.SubjectService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/subjects")
public class SubjectController {

    @Autowired
    private SubjectService subjectService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<Subject>>> getAllActiveSubjects(
            @RequestParam(value = "search", required = false) String search
    ) {
        List<Subject> subjects = subjectService.getAllSubjects(search);
        // Filter out inactive subjects
        List<Subject> activeSubjects = subjects.stream()
                .filter(s -> s.getIsActive() == null || s.getIsActive())
                .toList();
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách môn học thành công", activeSubjects));
    }
}
