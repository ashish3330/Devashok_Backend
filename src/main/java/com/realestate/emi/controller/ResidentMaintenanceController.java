package com.realestate.emi.controller;

import com.realestate.emi.dto.request.MaintenancePaymentRequest;
import com.realestate.emi.dto.response.ApiResponse;
import com.realestate.emi.dto.response.MaintenanceBillResponse;
import com.realestate.emi.dto.response.MaintenancePaymentResponse;
import com.realestate.emi.service.MaintenanceBillService;
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
@RequestMapping("/api/resident/maintenance")
@RequiredArgsConstructor
@PreAuthorize("hasRole('RESIDENT')")
public class ResidentMaintenanceController {

    private final MaintenanceBillService billService;

    @GetMapping("/bills")
    public ResponseEntity<ApiResponse<List<MaintenanceBillResponse>>> bills() {
        return ResponseEntity.ok(ApiResponse.success(billService.listForResident(), "Bills retrieved successfully"));
    }

    @GetMapping("/bills/{id}")
    public ResponseEntity<ApiResponse<MaintenanceBillResponse>> bill(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(billService.findByIdForResident(id), "Bill retrieved successfully"));
    }

    @PostMapping("/bills/{id}/pay")
    public ResponseEntity<ApiResponse<MaintenancePaymentResponse>> pay(
            @PathVariable Long id, @Valid @RequestBody MaintenancePaymentRequest request) {
        MaintenancePaymentResponse payment = billService.residentPay(id, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(payment, "Payment recorded"));
    }
}
