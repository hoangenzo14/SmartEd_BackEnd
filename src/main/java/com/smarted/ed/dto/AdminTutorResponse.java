package com.smarted.ed.dto;

import com.smarted.ed.enums.ApprovalStatus;
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
public class AdminTutorResponse {
    private Integer userId;
    private String fullName;
    private String email;
    private String phone;
    private String avatarUrl;
    private String bio;
    private String location;
    private BigDecimal hourlyRate;
    private BigDecimal averageRating;
    private ApprovalStatus approvalStatus;
    private String rejectReason;
    private List<TutorSubjectDto> subjects;
    private List<WeeklyScheduleSlotDto> bookedSlots;
}
