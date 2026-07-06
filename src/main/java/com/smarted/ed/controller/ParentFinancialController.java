package com.smarted.ed.controller;

import com.smarted.ed.dto.ApiResponse;
import com.smarted.ed.dto.ParentMonthlyStatsResponse;
import com.smarted.ed.dto.ParentStudentStatsResponse;
import com.smarted.ed.service.FinancialService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/parents/revenue")
public class ParentFinancialController {

    @Autowired
    private FinancialService financialService;

    @GetMapping("/students")
    public ResponseEntity<ApiResponse<List<ParentStudentStatsResponse>>> getParentStudentStats(
            @RequestParam(value = "year", required = false) Integer year
    ) {
        com.smarted.ed.entity.User principal = (com.smarted.ed.entity.User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        int filterYear = year != null ? year : LocalDate.now().getYear();
        List<ParentStudentStatsResponse> res = financialService.getParentStudentStats(principal.getId(), filterYear);
        return ResponseEntity.ok(ApiResponse.success("Lấy thống kê chi tiêu theo học sinh thành công", res));
    }

    @GetMapping("/monthly")
    public ResponseEntity<ApiResponse<List<ParentMonthlyStatsResponse>>> getParentMonthlyStats(
            @RequestParam(value = "year", required = false) Integer year
    ) {
        com.smarted.ed.entity.User principal = (com.smarted.ed.entity.User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        int filterYear = year != null ? year : LocalDate.now().getYear();
        List<ParentMonthlyStatsResponse> res = financialService.getParentMonthlyStats(principal.getId(), filterYear);
        return ResponseEntity.ok(ApiResponse.success("Lấy thống kê chi tiêu theo tháng thành công", res));
    }
}
