package com.smarted.ed.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TutorProfileDto {
    private Integer userId;
    private String fullName;
    private String email;
    private String phone;
    private String avatarUrl;
    private String bio;
    private String location;
    private BigDecimal hourlyRate;
    private BigDecimal averageRating;
    private List<String> subjects;
}
