package com.smarted.ed.service;

import com.smarted.ed.dto.*;
import java.util.List;

public interface FinancialService {
    TutorRevenueStatsResponse getTutorRevenueStats(Integer tutorId);
    List<AdminMonthlyRevenueResponse> getAdminMonthlyRevenue(int year);
    List<AdminSubjectRevenueResponse> getAdminSubjectRevenue(int year);
    List<ParentStudentStatsResponse> getParentStudentStats(Integer parentId, int year);
    List<ParentMonthlyStatsResponse> getParentMonthlyStats(Integer parentId, int year);
}
