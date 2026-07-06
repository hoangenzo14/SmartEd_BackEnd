package com.smarted.ed.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeedbackResponse {
    private Integer id;
    private Integer appointmentId;
    private Integer parentId;
    private String parentName;
    private Integer tutorId;
    private String tutorName;
    private Integer rating;
    private String comment;
    private LocalDateTime createdAt;
}
