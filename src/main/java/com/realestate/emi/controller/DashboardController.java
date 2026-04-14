package com.realestate.emi.controller;

import com.realestate.emi.dto.response.ApiResponse;
import com.realestate.emi.dto.response.ComprehensiveAnalyticsResponse;
import com.realestate.emi.dto.response.DashboardResponse;
import com.realestate.emi.dto.response.ExpenseDashboardResponse;
import com.realestate.emi.dto.response.MonthlyAnalyticsResponse;
import com.realestate.emi.service.DashboardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.YearMonth;

@Slf4j
@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','VIEWER')")
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<DashboardResponse>> getSummary() {
        DashboardResponse summary = dashboardService.getSummary();
        return ResponseEntity.ok(ApiResponse.success(summary, "Dashboard summary retrieved successfully"));
    }

    @GetMapping("/monthly-analytics")
    public ResponseEntity<ApiResponse<MonthlyAnalyticsResponse>> getMonthlyAnalytics(
            @RequestParam(defaultValue = "0") int year,
            @RequestParam(defaultValue = "0") int month) {
        YearMonth ym = (year == 0 || month == 0) ? YearMonth.now() : YearMonth.of(year, month);
        MonthlyAnalyticsResponse response = dashboardService.getMonthlyAnalytics(ym.getYear(), ym.getMonthValue());
        return ResponseEntity.ok(ApiResponse.success(response, "Monthly analytics retrieved successfully"));
    }

    @GetMapping("/analytics")
    public ResponseEntity<ApiResponse<ComprehensiveAnalyticsResponse>> getComprehensiveAnalytics() {
        ComprehensiveAnalyticsResponse response = dashboardService.getComprehensiveAnalytics();
        return ResponseEntity.ok(ApiResponse.success(response, "Analytics retrieved successfully"));
    }

    @GetMapping("/expenses")
    public ResponseEntity<ApiResponse<ExpenseDashboardResponse>> getExpenseDashboard() {
        ExpenseDashboardResponse response = dashboardService.getExpenseDashboard();
        return ResponseEntity.ok(ApiResponse.success(response, "Expense dashboard retrieved successfully"));
    }
}
