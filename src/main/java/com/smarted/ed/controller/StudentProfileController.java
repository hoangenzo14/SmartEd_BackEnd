package com.smarted.ed.controller;

import com.smarted.ed.configs.CustomUserDetails;
import com.smarted.ed.dto.ApiResponse;
import com.smarted.ed.dto.StudentProfileRequest;
import com.smarted.ed.entity.StudentProfile;
import com.smarted.ed.entity.User;
import com.smarted.ed.repository.UserRepository;
import com.smarted.ed.service.StudentProfileService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/parents/students")
public class StudentProfileController {

    @Autowired
    private StudentProfileService studentProfileService;

    @Autowired
    private UserRepository userRepository;

    private Integer getLoggedInParentId() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof CustomUserDetails) {
            return ((CustomUserDetails) principal).getId();
        }
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .map(User::getId)
                .orElseThrow(() -> new IllegalStateException("Người dùng chưa được xác thực hoặc thông tin xác thực không hợp lệ"));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<StudentProfile>>> getMyStudents() {
        List<StudentProfile> students = studentProfileService.getStudentsByParent(getLoggedInParentId());
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách hồ sơ học sinh thành công", students));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<StudentProfile>> createStudent(@Valid @ModelAttribute StudentProfileRequest request) {
        StudentProfile student = studentProfileService.createStudentProfile(getLoggedInParentId(), request);
        return ResponseEntity.ok(ApiResponse.success("Tạo hồ sơ học sinh thành công", student));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<StudentProfile>> updateStudent(
            @PathVariable("id") Integer id,
            @Valid @ModelAttribute StudentProfileRequest request
    ) {
        StudentProfile student = studentProfileService.updateStudentProfile(getLoggedInParentId(), id, request);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật hồ sơ học sinh thành công", student));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteStudent(@PathVariable("id") Integer id) {
        studentProfileService.deleteStudentProfile(getLoggedInParentId(), id);
        return ResponseEntity.ok(ApiResponse.success("Xóa hồ sơ học sinh thành công"));
    }
}
