package com.smarted.ed.controller;

import com.smarted.ed.dto.AdminMonthlyRevenueResponse;
import com.smarted.ed.dto.AdminSubjectRevenueResponse;
import com.smarted.ed.dto.ApiResponse;
import com.smarted.ed.service.FinancialService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/admin/revenue")
public class AdminFinancialController {

    @Autowired
    private FinancialService financialService;

    @GetMapping("/monthly")
    public ResponseEntity<ApiResponse<List<AdminMonthlyRevenueResponse>>> getAdminMonthlyRevenue(
            @RequestParam(value = "year", required = false) Integer year
    ) {
        int filterYear = year != null ? year : LocalDate.now().getYear();
        List<AdminMonthlyRevenueResponse> res = financialService.getAdminMonthlyRevenue(filterYear);
        return ResponseEntity.ok(ApiResponse.success("Lấy thống kê doanh thu theo tháng thành công", res));
    }

    @GetMapping("/subjects")
    public ResponseEntity<ApiResponse<List<AdminSubjectRevenueResponse>>> getAdminSubjectRevenue(
            @RequestParam(value = "year", required = false) Integer year
    ) {
        int filterYear = year != null ? year : LocalDate.now().getYear();
        List<AdminSubjectRevenueResponse> res = financialService.getAdminSubjectRevenue(filterYear);
        return ResponseEntity.ok(ApiResponse.success("Lấy thống kê doanh thu theo môn học thành công", res));
    }
}
