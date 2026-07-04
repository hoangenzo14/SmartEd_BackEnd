// com.example.demo.repository.AppointmentRepository.java
package com.smarted.ed.repository;

import com.smarted.ed.entity.Appointment;
import com.smarted.ed.enums.AppointmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, Integer> {
    List<Appointment> findByParentIdAndTimeRange(
            @Param("parentId") Integer parentId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );

    //
    List<Appointment> findByTutorId(Integer tutorId);
    List<Appointment> findByStudentId(Integer studentId);
    List<Appointment> findByStatus(AppointmentStatus status);
}