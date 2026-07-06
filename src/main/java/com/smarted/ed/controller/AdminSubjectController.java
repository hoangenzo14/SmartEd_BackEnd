package com.smarted.ed.controller;

import com.smarted.ed.dto.ApiResponse;
import com.smarted.ed.dto.SubjectRequest;
import com.smarted.ed.entity.Subject;
import com.smarted.ed.service.SubjectService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/subjects")
public class AdminSubjectController {

    @Autowired
    private SubjectService subjectService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<Subject>>> getAllSubjects(
            @RequestParam(value = "search", required = false) String search
    ) {
        List<Subject> subjects = subjectService.getAllSubjects(search);
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách môn học thành công", subjects));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Subject>> getSubjectById(@PathVariable("id") Integer id) {
        Subject subject = subjectService.getSubjectById(id);
        return ResponseEntity.ok(ApiResponse.success("Lấy chi tiết môn học thành công", subject));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Subject>> createSubject(@Valid @RequestBody SubjectRequest request) {
        Subject subject = subjectService.createSubject(request);
        return ResponseEntity.ok(ApiResponse.success("Tạo môn học thành công", subject));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Subject>> updateSubject(
            @PathVariable("id") Integer id,
            @Valid @RequestBody SubjectRequest request
    ) {
        Subject subject = subjectService.updateSubject(id, request);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật môn học thành công", subject));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteSubject(@PathVariable("id") Integer id) {
        subjectService.deleteSubject(id);
        return ResponseEntity.ok(ApiResponse.success("Xóa môn học thành công"));
    }
}
