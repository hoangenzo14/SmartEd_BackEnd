package com.smarted.ed.dto;

import com.smarted.ed.enums.AppointmentStatus;
import com.smarted.ed.enums.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AppointmentResponse {
    private Integer id;
    private Integer studentId;
    private String studentName;
    private Integer tutorId;
    private String tutorName;
    private String tutorAvatar;
    private Integer subjectId;
    private String subjectName;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private AppointmentStatus status;
    private PaymentStatus paymentStatus;
    private String cancellationReason;
    private BigDecimal totalPrice;
    private String paymentUrl;
    private Integer feedbackId;
    private LocalDateTime createdAt;
}
