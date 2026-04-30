package com.realestate.emi.controller;

import com.realestate.emi.dto.request.MaintenanceBillGenerateRequest;
import com.realestate.emi.dto.request.MaintenancePaymentRequest;
import com.realestate.emi.dto.response.ApiResponse;
import com.realestate.emi.dto.response.MaintenanceBillResponse;
import com.realestate.emi.dto.response.MaintenancePaymentResponse;
import com.realestate.emi.enums.MaintenanceBillStatus;
import com.realestate.emi.service.MaintenanceBillService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/society/maintenance-bills")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','SUPERVISOR')")
public class MaintenanceBillController {

    private final MaintenanceBillService billService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<MaintenanceBillResponse>>> findAll(
            @RequestParam(required = false) Long blockId,
            @RequestParam(required = false) Long flatId,
            @RequestParam(required = false) MaintenanceBillStatus status,
            @RequestParam(required = false) String billMonth,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(ApiResponse.success(
                billService.listAdmin(blockId, flatId, status, billMonth, from, to),
                "Bills retrieved successfully"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<MaintenanceBillResponse>> findById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(billService.findByIdAdmin(id), "Bill retrieved successfully"));
    }

    @PostMapping("/generate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<MaintenanceBillResponse>>> generate(@Valid @RequestBody MaintenanceBillGenerateRequest request) {
        List<MaintenanceBillResponse> created = billService.generateBills(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(created, "Bills generated"));
    }

    @PostMapping("/{id}/payments")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<MaintenancePaymentResponse>> recordPayment(
            @PathVariable Long id, @Valid @RequestBody MaintenancePaymentRequest request) {
        MaintenancePaymentResponse payment = billService.adminRecordPayment(id, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(payment, "Payment recorded"));
    }
}
