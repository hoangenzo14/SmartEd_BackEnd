package com.smarted.ed.dto;

import com.smarted.ed.enums.ApprovalStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TutorSubjectDto {
    private Integer subjectId;
    private String subjectName;
    private ApprovalStatus status;
    private String certificateUrl;
    private String rejectReason;
    private String bio;
    private BigDecimal hourlyRate;
}
