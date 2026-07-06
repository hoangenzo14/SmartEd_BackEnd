package com.smarted.ed.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class TutorSubjectRegisterRequest {
    @NotNull(message = "ID môn học không được để trống")
    private Integer subjectId;

    @NotBlank(message = "Giới thiệu môn học không được để trống")
    private String bio;

    @NotNull(message = "Giá theo giờ không được để trống")
    private BigDecimal hourlyRate;

    @NotBlank(message = "Đường dẫn chứng chỉ không được để trống")
    private String certificateUrl;
}
