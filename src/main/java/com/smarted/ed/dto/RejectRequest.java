package com.smarted.ed.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RejectRequest {
    @NotBlank(message = "Lý do từ chối không được để trống")
    private String reason;
}
