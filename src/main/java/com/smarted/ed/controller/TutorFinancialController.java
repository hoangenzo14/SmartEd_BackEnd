package com.smarted.ed.controller;

import com.smarted.ed.dto.ApiResponse;
import com.smarted.ed.dto.TutorRevenueStatsResponse;
import com.smarted.ed.service.FinancialService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tutors/revenue")
public class TutorFinancialController {

    @Autowired
    private FinancialService financialService;

    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<TutorRevenueStatsResponse>> getTutorRevenueStats() {
        com.smarted.ed.entity.User principal = (com.smarted.ed.entity.User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        TutorRevenueStatsResponse res = financialService.getTutorRevenueStats(principal.getId());
        return ResponseEntity.ok(ApiResponse.success("Lấy thống kê doanh thu gia sư thành công", res));
    }
}
