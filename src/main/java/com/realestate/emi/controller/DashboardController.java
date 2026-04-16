package com.realestate.emi.controller;

import com.realestate.emi.dto.response.ApiResponse;
import com.realestate.emi.dto.response.ComprehensiveAnalyticsResponse;
import com.realestate.emi.dto.response.DashboardResponse;
import com.realestate.emi.dto.response.ExpenseDashboardResponse;
import com.realestate.emi.dto.response.MonthlyAnalyticsResponse;
import com.realestate.emi.dto.response.NotificationResponse;
import com.realestate.emi.dto.response.StaffAnalyticsResponse;
import com.realestate.emi.service.DashboardService;
import com.realestate.emi.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;


import java.time.YearMonth;

@Slf4j
@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class DashboardController {

    private final DashboardService dashboardService;
    private final NotificationService notificationService;

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
    public ResponseEntity<ApiResponse<ExpenseDashboardResponse>> getExpenseDashboard(
            @RequestParam(defaultValue = "0") int year,
            @RequestParam(defaultValue = "0") int month) {
        YearMonth ym = (year == 0 || month == 0) ? YearMonth.now() : YearMonth.of(year, month);
        ExpenseDashboardResponse response = dashboardService.getExpenseDashboard(ym.getYear(), ym.getMonthValue());
        return ResponseEntity.ok(ApiResponse.success(response, "Expense dashboard retrieved successfully"));
    }

    @GetMapping("/staff-analytics")
    public ResponseEntity<ApiResponse<StaffAnalyticsResponse>> getStaffAnalytics() {
        StaffAnalyticsResponse response = dashboardService.getStaffAnalytics();
        return ResponseEntity.ok(ApiResponse.success(response, "Staff analytics retrieved successfully"));
    }

    @GetMapping("/notifications")
    @PreAuthorize("hasAnyRole('ADMIN','SUPERVISOR')")
    public ResponseEntity<ApiResponse<NotificationResponse>> getNotifications() {
        NotificationResponse response = notificationService.getNotifications();
        return ResponseEntity.ok(ApiResponse.success(response, "Notifications retrieved"));
    }

    @PutMapping("/notifications/{id}/read")
    @PreAuthorize("hasAnyRole('ADMIN','SUPERVISOR')")
    public ResponseEntity<ApiResponse<Void>> markNotificationRead(@PathVariable Long id) {
        notificationService.markAsRead(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Notification marked as read"));
    }

    @PutMapping("/notifications/read-all")
    @PreAuthorize("hasAnyRole('ADMIN','SUPERVISOR')")
    public ResponseEntity<ApiResponse<Void>> markAllNotificationsRead() {
        notificationService.markAllAsRead();
        return ResponseEntity.ok(ApiResponse.success(null, "All notifications marked as read"));
    }
}
