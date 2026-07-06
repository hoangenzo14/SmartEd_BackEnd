package com.smarted.ed.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AppointmentRequest {
    @NotNull(message = "Học sinh không được để trống")
    private Integer studentId;

    @NotNull(message = "Gia sư không được để trống")
    private Integer tutorId;

    @NotNull(message = "Môn học không được để trống")
    private Integer subjectId;

    @NotNull(message = "Thời gian bắt đầu không được để trống")
    private LocalDateTime startTime;

    @NotNull(message = "Thời gian kết thúc không được để trống")
    private LocalDateTime endTime;

    private String notes;
}
