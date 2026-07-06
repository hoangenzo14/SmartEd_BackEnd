package com.smarted.ed.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SubjectRequest {
    @NotBlank(message = "Tên môn học không được để trống")
    private String name;

    private String description;

    private Boolean isActive = true;
}
