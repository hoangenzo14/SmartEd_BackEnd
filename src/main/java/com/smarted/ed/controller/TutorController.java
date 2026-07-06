package com.smarted.ed.controller;

import com.smarted.ed.configs.CustomUserDetails;
import com.smarted.ed.dto.ApiResponse;
import com.smarted.ed.dto.AdminTutorResponse;
import com.smarted.ed.dto.TutorProfileDto;
import com.smarted.ed.entity.User;
import com.smarted.ed.repository.UserRepository;
import com.smarted.ed.service.TutorService;
import com.smarted.ed.dto.TutorSubjectDto;
import com.smarted.ed.dto.TutorSubjectRegisterRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/tutors")
public class TutorController {

    @Autowired
    private TutorService tutorService;

    @Autowired
    private UserRepository userRepository;

    @GetMapping({"", "/approved"})
    public ResponseEntity<ApiResponse<Page<TutorProfileDto>>> getApprovedTutors(
            @RequestParam(value = "search", required = false, defaultValue = "") String search,
            @RequestParam(value = "page", required = false, defaultValue = "0") int page,
            @RequestParam(value = "size", required = false, defaultValue = "10") int size,
            @RequestParam(value = "sortBy", required = false, defaultValue = "rating") String sortBy,
            @RequestParam(value = "sortDir", required = false, defaultValue = "desc") String sortDir
    ) {
        Page<TutorProfileDto> tutors = tutorService.getApprovedTutors(search, page, size, sortBy, sortDir);
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách gia sư thành công", tutors));
    }

    private Integer getLoggedInTutorId() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof CustomUserDetails) {
            return ((CustomUserDetails) principal).getId();
        }
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .map(User::getId)
                .orElseThrow(() -> new IllegalStateException("Người dùng chưa được xác thực hoặc thông tin xác thực không hợp lệ"));
    }

    @GetMapping("/my-subjects")
    @PreAuthorize("hasRole('TUTOR')")
    public ResponseEntity<ApiResponse<List<TutorSubjectDto>>> getMySubjects() {
        List<TutorSubjectDto> subjects = tutorService.getMySubjects(getLoggedInTutorId());
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách môn học đăng ký thành công", subjects));
    }

    @PostMapping("/my-subjects")
    @PreAuthorize("hasRole('TUTOR')")
    public ResponseEntity<ApiResponse<Void>> registerSubject(@Valid @RequestBody TutorSubjectRegisterRequest request) {
        tutorService.registerSubject(getLoggedInTutorId(), request);
        return ResponseEntity.ok(ApiResponse.success("Đăng ký môn học thành công. Vui lòng chờ admin phê duyệt."));
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<Page<TutorProfileDto>>> searchTutors(
            @RequestParam(value = "search", required = false, defaultValue = "") String search,
            @RequestParam(value = "subjectId", required = false) Integer subjectId,
            @RequestParam(value = "minPrice", required = false) java.math.BigDecimal minPrice,
            @RequestParam(value = "maxPrice", required = false) java.math.BigDecimal maxPrice,
            @RequestParam(value = "minRating", required = false) java.math.BigDecimal minRating,
            @RequestParam(value = "page", required = false, defaultValue = "0") int page,
            @RequestParam(value = "size", required = false, defaultValue = "10") int size,
            @RequestParam(value = "sortBy", required = false, defaultValue = "rating") String sortBy,
            @RequestParam(value = "sortDir", required = false, defaultValue = "desc") String sortDir
    ) {
        Page<TutorProfileDto> tutors = tutorService.searchTutors(
                search, subjectId, minPrice, maxPrice, minRating, page, size, sortBy, sortDir);
        return ResponseEntity.ok(ApiResponse.success("Tìm kiếm gia sư thành công", tutors));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<AdminTutorResponse>> getTutorById(@PathVariable("id") Integer id) {
        AdminTutorResponse tutor = tutorService.getTutorDetails(id);
        return ResponseEntity.ok(ApiResponse.success("Lấy chi tiết gia sư thành công", tutor));
    }

    @PutMapping("/profile")
    public ResponseEntity<ApiResponse<TutorProfileDto>> updateProfile(
            @ModelAttribute com.smarted.ed.dto.TutorProfileUpdateRequest request
    ) {
        TutorProfileDto updated = tutorService.updateProfile(getLoggedInTutorId(), request);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật hồ sơ thành công", updated));
    }
}
