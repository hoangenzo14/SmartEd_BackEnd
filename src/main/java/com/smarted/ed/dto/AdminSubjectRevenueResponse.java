package com.smarted.ed.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminSubjectRevenueResponse {
    private Integer subjectId;
    private String subjectName;
    private BigDecimal revenue;
    private double percentage;
}
