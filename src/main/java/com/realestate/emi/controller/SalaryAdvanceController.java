package com.realestate.emi.controller;

import com.realestate.emi.dto.request.SalaryAdvanceRequest;
import com.realestate.emi.dto.response.ApiResponse;
import com.realestate.emi.dto.response.SalaryAdvanceResponse;
import com.realestate.emi.service.SalaryAdvanceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/staff/advances")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','SUPERVISOR')")
public class SalaryAdvanceController {

    private final SalaryAdvanceService salaryAdvanceService;

    @PostMapping("/{staffId}")
    public ResponseEntity<ApiResponse<SalaryAdvanceResponse>> grantAdvance(
            @PathVariable Long staffId,
            @Valid @RequestBody SalaryAdvanceRequest request) {
        SalaryAdvanceResponse response = salaryAdvanceService.grantAdvance(staffId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Salary advance granted successfully"));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<SalaryAdvanceResponse>>> getActiveAdvances() {
        return ResponseEntity.ok(ApiResponse.success(salaryAdvanceService.getActiveAdvances(), "Active advances retrieved successfully"));
    }

    @GetMapping("/staff/{staffId}")
    public ResponseEntity<ApiResponse<List<SalaryAdvanceResponse>>> getAdvancesByStaff(@PathVariable Long staffId) {
        return ResponseEntity.ok(ApiResponse.success(salaryAdvanceService.getAdvancesByStaff(staffId), "Staff advances retrieved successfully"));
    }

    @PutMapping("/{advanceId}/cancel")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<SalaryAdvanceResponse>> cancelAdvance(@PathVariable Long advanceId) {
        return ResponseEntity.ok(ApiResponse.success(salaryAdvanceService.cancelAdvance(advanceId), "Advance cancelled successfully"));
    }
}
