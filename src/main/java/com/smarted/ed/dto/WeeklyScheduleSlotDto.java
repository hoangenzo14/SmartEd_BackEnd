package com.smarted.ed.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WeeklyScheduleSlotDto {
    private String dayOfWeek; // e.g. "Monday", "Tuesday"...
    private String slotTime;  // e.g. "08:00", "10:00"...
    private String status;    // "BOOKED"
}
