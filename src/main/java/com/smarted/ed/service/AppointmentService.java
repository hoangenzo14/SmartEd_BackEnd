package com.smarted.ed.service;

import com.smarted.ed.dto.AppointmentRequest;
import com.smarted.ed.dto.AppointmentResponse;
import java.util.List;

public interface AppointmentService {
    AppointmentResponse createAppointment(Integer parentId, AppointmentRequest request);
    AppointmentResponse acceptAppointment(Integer tutorId, Integer appointmentId);
    AppointmentResponse rejectAppointment(Integer tutorId, Integer appointmentId, String reason);
    AppointmentResponse completeAppointment(Integer tutorId, Integer appointmentId);
    org.springframework.data.domain.Page<AppointmentResponse> getAppointmentsForTutor(
            Integer tutorId, String status, String month, org.springframework.data.domain.Pageable pageable);
    org.springframework.data.domain.Page<AppointmentResponse> getAppointmentsForParent(
            Integer parentId, Integer studentId, String status, String month, org.springframework.data.domain.Pageable pageable);
    void processVNPayCallback(java.util.Map<String, String> params);
    AppointmentResponse cancelAppointment(Integer parentId, Integer appointmentId, String reason);
    java.util.List<AppointmentResponse> getCancelledOrRejectedPaidAppointments();
}
