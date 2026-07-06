package com.smarted.ed.controller;

import com.smarted.ed.dto.ApiResponse;
import com.smarted.ed.dto.AppointmentResponse;
import com.smarted.ed.service.AppointmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/appointments")
public class AdminAppointmentController {

    @Autowired
    private AppointmentService appointmentService;

    @GetMapping("/cancelled-rejected")
    public ResponseEntity<ApiResponse<List<AppointmentResponse>>> getCancelledOrRejectedPaidAppointments() {
        List<AppointmentResponse> res = appointmentService.getCancelledOrRejectedPaidAppointments();
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách lịch học bị hủy/từ chối thành công", res));
    }
}
