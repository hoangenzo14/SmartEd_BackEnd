package com.smarted.ed.controller;

import com.smarted.ed.dto.AdminTutorResponse;
import com.smarted.ed.dto.ApiResponse;
import com.smarted.ed.dto.RejectRequest;
import com.smarted.ed.enums.ApprovalStatus;
import com.smarted.ed.service.AdminService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/tutors")
public class AdminController {

    @Autowired
    private AdminService adminService;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<AdminTutorResponse>>> getAllTutors(
            @RequestParam(value = "status", required = false) ApprovalStatus status,
            @RequestParam(value = "search", required = false, defaultValue = "") String search,
            @RequestParam(value = "page", required = false, defaultValue = "0") int page,
            @RequestParam(value = "size", required = false, defaultValue = "10") int size,
            @RequestParam(value = "sortBy", required = false, defaultValue = "userId") String sortBy,
            @RequestParam(value = "sortDir", required = false, defaultValue = "desc") String sortDir
    ) {
        Page<AdminTutorResponse> tutors = adminService.getAllTutors(status, search, page, size, sortBy, sortDir);
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách gia sư thành công", tutors));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<AdminTutorResponse>> getTutorById(@PathVariable("id") Integer id) {
        AdminTutorResponse tutor = adminService.getTutorById(id);
        return ResponseEntity.ok(ApiResponse.success("Lấy chi tiết gia sư thành công", tutor));
    }


    @PutMapping("/{tutorId}/subjects/{subjectId}/approve")
    public ResponseEntity<ApiResponse<Void>> approveTutorSubject(
            @PathVariable("tutorId") Integer tutorId,
            @PathVariable("subjectId") Integer subjectId
    ) {
        adminService.approveTutorSubject(tutorId, subjectId);
        return ResponseEntity.ok(ApiResponse.success("Duyệt môn học của gia sư thành công"));
    }

    @PutMapping("/{tutorId}/subjects/{subjectId}/reject")
    public ResponseEntity<ApiResponse<Void>> rejectTutorSubject(
            @PathVariable("tutorId") Integer tutorId,
            @PathVariable("subjectId") Integer subjectId,
            @Valid @RequestBody RejectRequest request
    ) {
        adminService.rejectTutorSubject(tutorId, subjectId, request.getReason());
        return ResponseEntity.ok(ApiResponse.success("Từ chối môn học của gia sư thành công"));
    }

    @PutMapping("/{id}/approve")
    public ResponseEntity<ApiResponse<Void>> approveTutor(@PathVariable("id") Integer id) {
        adminService.approveTutor(id);
        return ResponseEntity.ok(ApiResponse.success("Duyệt gia sư thành công"));
    }

    @PutMapping("/{id}/reject")
    public ResponseEntity<ApiResponse<Void>> rejectTutor(
            @PathVariable("id") Integer id,
            @Valid @RequestBody RejectRequest request
    ) {
        adminService.rejectTutor(id, request.getReason());
        return ResponseEntity.ok(ApiResponse.success("Từ chối gia sư thành công"));
    }
}
