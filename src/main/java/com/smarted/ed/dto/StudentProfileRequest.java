package com.smarted.ed.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
public class StudentProfileRequest {
    @NotBlank(message = "Họ tên học sinh không được để trống")
    private String fullName;

    @NotNull(message = "Khối lớp không được để trống")
    private Integer grade;

    private String schoolName;
    private String academicPerformance;
    private String notes;
    private String avatarUrl;
    private MultipartFile file;
}
