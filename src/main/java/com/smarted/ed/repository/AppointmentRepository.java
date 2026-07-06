// com.example.demo.repository.AppointmentRepository.java
package com.smarted.ed.repository;

import com.smarted.ed.entity.Appointment;
import com.smarted.ed.enums.AppointmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, Integer> {


    //
    List<Appointment> findByTutorId(Integer tutorId);
    List<Appointment> findByStudentId(Integer studentId);
    List<Appointment> findByStudentIdIn(List<Integer> studentIds);
    List<Appointment> findByStatus(AppointmentStatus status);
    List<Appointment> findByStatusInAndPaymentStatus(List<AppointmentStatus> statuses, com.smarted.ed.enums.PaymentStatus paymentStatus);

    @org.springframework.data.jpa.repository.Query(
        "SELECT a FROM Appointment a WHERE a.tutorId = :tutorId " +
        "AND (:status IS NULL OR a.status = :status) " +
        "AND (:month IS NULL OR FUNCTION('MONTH', a.startTime) = :month) " +
        "AND (:year IS NULL OR FUNCTION('YEAR', a.startTime) = :year)"
    )
    org.springframework.data.domain.Page<Appointment> findForTutorWithFilters(
        @org.springframework.data.repository.query.Param("tutorId") Integer tutorId,
        @org.springframework.data.repository.query.Param("status") AppointmentStatus status,
        @org.springframework.data.repository.query.Param("month") Integer month,
        @org.springframework.data.repository.query.Param("year") Integer year,
        org.springframework.data.domain.Pageable pageable
    );

    @org.springframework.data.jpa.repository.Query(
        "SELECT a FROM Appointment a WHERE a.studentId IN :studentIds " +
        "AND (:filterStudentId IS NULL OR a.studentId = :filterStudentId) " +
        "AND (:status IS NULL OR a.status = :status) " +
        "AND (:month IS NULL OR FUNCTION('MONTH', a.startTime) = :month) " +
        "AND (:year IS NULL OR FUNCTION('YEAR', a.startTime) = :year)"
    )
    org.springframework.data.domain.Page<Appointment> findForParentWithFilters(
        @org.springframework.data.repository.query.Param("studentIds") List<Integer> studentIds,
        @org.springframework.data.repository.query.Param("filterStudentId") Integer filterStudentId,
        @org.springframework.data.repository.query.Param("status") AppointmentStatus status,
        @org.springframework.data.repository.query.Param("month") Integer month,
        @org.springframework.data.repository.query.Param("year") Integer year,
        org.springframework.data.domain.Pageable pageable
    );

    @org.springframework.data.jpa.repository.Query(
        "SELECT COUNT(a) FROM Appointment a WHERE a.tutorId = :tutorId " +
        "AND a.status != 'CANCELLED' " +
        "AND (:startTime < a.endTime AND :endTime > a.startTime)"
    )
    long countOverlappingAppointments(
        @org.springframework.data.repository.query.Param("tutorId") Integer tutorId,
        @org.springframework.data.repository.query.Param("startTime") java.time.LocalDateTime startTime,
        @org.springframework.data.repository.query.Param("endTime") java.time.LocalDateTime endTime
    );

    @org.springframework.data.jpa.repository.Query(
        "SELECT COALESCE(SUM(a.totalPrice), 0) FROM Appointment a " +
        "WHERE a.tutorId = :tutorId " +
        "AND (a.paymentStatus = com.smarted.ed.enums.PaymentStatus.PAID OR a.status = com.smarted.ed.enums.AppointmentStatus.COMPLETED) " +
        "AND a.startTime >= :startDateTime AND a.startTime <= :endDateTime"
    )
    java.math.BigDecimal sumTutorRevenueForPeriod(
        @org.springframework.data.repository.query.Param("tutorId") Integer tutorId,
        @org.springframework.data.repository.query.Param("startDateTime") java.time.LocalDateTime startDateTime,
        @org.springframework.data.repository.query.Param("endDateTime") java.time.LocalDateTime endDateTime
    );

    @org.springframework.data.jpa.repository.Query(
        "SELECT COALESCE(SUM(a.totalPrice), 0) FROM Appointment a " +
        "WHERE a.tutorId = :tutorId " +
        "AND (a.paymentStatus = com.smarted.ed.enums.PaymentStatus.PAID OR a.status = com.smarted.ed.enums.AppointmentStatus.COMPLETED)"
    )
    java.math.BigDecimal sumTotalTutorRevenue(
        @org.springframework.data.repository.query.Param("tutorId") Integer tutorId
    );

    @org.springframework.data.jpa.repository.Query(
        "SELECT DISTINCT a FROM Appointment a " +
        "LEFT JOIN FETCH a.subject " +
        "WHERE (a.paymentStatus = com.smarted.ed.enums.PaymentStatus.PAID OR a.status = com.smarted.ed.enums.AppointmentStatus.COMPLETED) " +
        "AND a.startTime >= :startDateTime AND a.startTime <= :endDateTime"
    )
    java.util.List<Appointment> findAllValidAppointmentsForPeriod(
        @org.springframework.data.repository.query.Param("startDateTime") java.time.LocalDateTime startDateTime,
        @org.springframework.data.repository.query.Param("endDateTime") java.time.LocalDateTime endDateTime
    );

    @org.springframework.data.jpa.repository.Query(
        "SELECT DISTINCT a FROM Appointment a " +
        "LEFT JOIN FETCH a.student " +
        "WHERE a.student.parentId = :parentId " +
        "AND (a.paymentStatus = com.smarted.ed.enums.PaymentStatus.PAID OR a.status = com.smarted.ed.enums.AppointmentStatus.COMPLETED) " +
        "AND a.startTime >= :startDateTime AND a.startTime <= :endDateTime"
    )
    java.util.List<Appointment> findValidParentAppointmentsForPeriod(
        @org.springframework.data.repository.query.Param("parentId") Integer parentId,
        @org.springframework.data.repository.query.Param("startDateTime") java.time.LocalDateTime startDateTime,
        @org.springframework.data.repository.query.Param("endDateTime") java.time.LocalDateTime endDateTime
    );
}