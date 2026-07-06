package com.smarted.ed.service;

import com.smarted.ed.dto.*;
import com.smarted.ed.entity.Appointment;
import com.smarted.ed.repository.AppointmentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class FinancialServiceImpl implements FinancialService {

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Override
    public TutorRevenueStatsResponse getTutorRevenueStats(Integer tutorId) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startOfMonth = now.withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime endOfMonth = now.withDayOfMonth(now.toLocalDate().lengthOfMonth()).withHour(23).withMinute(59).withSecond(59).withNano(999999999);

        BigDecimal currentMonthRevenue = appointmentRepository.sumTutorRevenueForPeriod(tutorId, startOfMonth, endOfMonth);
        BigDecimal totalRevenue = appointmentRepository.sumTotalTutorRevenue(tutorId);

        return TutorRevenueStatsResponse.builder()
                .currentMonthRevenue(currentMonthRevenue != null ? currentMonthRevenue : BigDecimal.ZERO)
                .totalRevenue(totalRevenue != null ? totalRevenue : BigDecimal.ZERO)
                .build();
    }

    @Override
    public List<AdminMonthlyRevenueResponse> getAdminMonthlyRevenue(int year) {
        LocalDateTime startOfYear = LocalDateTime.of(year, 1, 1, 0, 0, 0);
        LocalDateTime endOfYear = LocalDateTime.of(year, 12, 31, 23, 59, 59, 999999999);

        List<Appointment> appointments = appointmentRepository.findAllValidAppointmentsForPeriod(startOfYear, endOfYear);
        if (appointments == null) {
            appointments = new ArrayList<>();
        }

        // Initialize 12 months
        Map<Integer, BigDecimal> monthlyMap = new HashMap<>();
        for (int m = 1; m <= 12; m++) {
            monthlyMap.put(m, BigDecimal.ZERO);
        }

        // Aggregate
        for (Appointment app : appointments) {
            if (app == null || app.getStartTime() == null) continue;
            int month = app.getStartTime().getMonthValue();
            BigDecimal price = app.getTotalPrice() != null ? app.getTotalPrice() : BigDecimal.ZERO;
            monthlyMap.put(month, monthlyMap.get(month).add(price));
        }

        List<AdminMonthlyRevenueResponse> result = new ArrayList<>();
        for (int m = 1; m <= 12; m++) {
            result.add(AdminMonthlyRevenueResponse.builder()
                    .month(m)
                    .revenue(monthlyMap.get(m))
                    .build());
        }

        return result;
    }

    @Override
    public List<AdminSubjectRevenueResponse> getAdminSubjectRevenue(int year) {
        LocalDateTime startOfYear = LocalDateTime.of(year, 1, 1, 0, 0, 0);
        LocalDateTime endOfYear = LocalDateTime.of(year, 12, 31, 23, 59, 59, 999999999);

        List<Appointment> appointments = appointmentRepository.findAllValidAppointmentsForPeriod(startOfYear, endOfYear);
        if (appointments == null) {
            appointments = new ArrayList<>();
        }

        // Group by subject
        Map<Integer, BigDecimal> subjectRevenueMap = new HashMap<>();
        Map<Integer, String> subjectNameMap = new HashMap<>();

        BigDecimal totalRevenue = BigDecimal.ZERO;

        for (Appointment app : appointments) {
            if (app == null) continue;
            Integer subId = app.getSubjectId();
            if (subId == null) continue;

            String subName = app.getSubject() != null ? app.getSubject().getName() : "Không xác định";
            BigDecimal price = app.getTotalPrice() != null ? app.getTotalPrice() : BigDecimal.ZERO;

            subjectRevenueMap.put(subId, subjectRevenueMap.getOrDefault(subId, BigDecimal.ZERO).add(price));
            subjectNameMap.put(subId, subName);
            totalRevenue = totalRevenue.add(price);
        }

        List<AdminSubjectRevenueResponse> result = new ArrayList<>();
        for (Map.Entry<Integer, BigDecimal> entry : subjectRevenueMap.entrySet()) {
            Integer subId = entry.getKey();
            BigDecimal rev = entry.getValue() != null ? entry.getValue() : BigDecimal.ZERO;
            double pct = 0.0;
            if (totalRevenue != null && totalRevenue.compareTo(BigDecimal.ZERO) > 0) {
                pct = rev.multiply(BigDecimal.valueOf(100))
                        .divide(totalRevenue, 2, RoundingMode.HALF_UP)
                        .doubleValue();
            }

            result.add(AdminSubjectRevenueResponse.builder()
                    .subjectId(subId)
                    .subjectName(subjectNameMap.get(subId))
                    .revenue(rev)
                    .percentage(pct)
                    .build());
        }

        // Sort descending by revenue
        result.sort((r1, r2) -> {
            BigDecimal rev1 = r1.getRevenue() != null ? r1.getRevenue() : BigDecimal.ZERO;
            BigDecimal rev2 = r2.getRevenue() != null ? r2.getRevenue() : BigDecimal.ZERO;
            return rev2.compareTo(rev1);
        });
        return result;
    }

    @Override
    public List<ParentStudentStatsResponse> getParentStudentStats(Integer parentId, int year) {
        LocalDateTime startOfYear = LocalDateTime.of(year, 1, 1, 0, 0, 0);
        LocalDateTime endOfYear = LocalDateTime.of(year, 12, 31, 23, 59, 59, 999999999);

        List<Appointment> appointments = appointmentRepository.findValidParentAppointmentsForPeriod(parentId, startOfYear, endOfYear);

        Map<Integer, BigDecimal> studentSpentMap = new HashMap<>();
        Map<Integer, String> studentNameMap = new HashMap<>();

        for (Appointment app : appointments) {
            Integer studentId = app.getStudentId();
            String name = app.getStudent() != null ? app.getStudent().getFullName() : "Học sinh";
            BigDecimal price = app.getTotalPrice() != null ? app.getTotalPrice() : BigDecimal.ZERO;

            studentSpentMap.put(studentId, studentSpentMap.getOrDefault(studentId, BigDecimal.ZERO).add(price));
            studentNameMap.put(studentId, name);
        }

        List<ParentStudentStatsResponse> result = new ArrayList<>();
        for (Map.Entry<Integer, BigDecimal> entry : studentSpentMap.entrySet()) {
            result.add(ParentStudentStatsResponse.builder()
                    .studentId(entry.getKey())
                    .studentName(studentNameMap.get(entry.getKey()))
                    .totalSpent(entry.getValue())
                    .build());
        }

        // Sort descending
        result.sort((s1, s2) -> s2.getTotalSpent().compareTo(s1.getTotalSpent()));
        return result;
    }

    @Override
    public List<ParentMonthlyStatsResponse> getParentMonthlyStats(Integer parentId, int year) {
        LocalDateTime startOfYear = LocalDateTime.of(year, 1, 1, 0, 0, 0);
        LocalDateTime endOfYear = LocalDateTime.of(year, 12, 31, 23, 59, 59, 999999999);

        List<Appointment> appointments = appointmentRepository.findValidParentAppointmentsForPeriod(parentId, startOfYear, endOfYear);

        Map<Integer, BigDecimal> monthlyMap = new HashMap<>();
        for (int m = 1; m <= 12; m++) {
            monthlyMap.put(m, BigDecimal.ZERO);
        }

        for (Appointment app : appointments) {
            int month = app.getStartTime().getMonthValue();
            BigDecimal price = app.getTotalPrice() != null ? app.getTotalPrice() : BigDecimal.ZERO;
            monthlyMap.put(month, monthlyMap.get(month).add(price));
        }

        List<ParentMonthlyStatsResponse> result = new ArrayList<>();
        for (int m = 1; m <= 12; m++) {
            result.add(ParentMonthlyStatsResponse.builder()
                    .month(m)
                    .totalSpent(monthlyMap.get(m))
                    .build());
        }

        return result;
    }
}
