package com.realestate.emi.controller;

import com.realestate.emi.dto.request.PaymentRequest;
import com.realestate.emi.dto.response.ApiResponse;
import com.realestate.emi.dto.response.InstallmentPhaseResponse;
import com.realestate.emi.dto.response.PaymentResponse;
import com.realestate.emi.service.InstallmentPhaseService;
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
@RequestMapping("/api/deals/{dealId}/phases")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class InstallmentPhaseController {

    private final InstallmentPhaseService installmentPhaseService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','VIEWER')")
    public ResponseEntity<ApiResponse<List<InstallmentPhaseResponse>>> getPhases(@PathVariable Long dealId) {
        List<InstallmentPhaseResponse> phases = installmentPhaseService.getPhasesByDeal(dealId);
        return ResponseEntity.ok(ApiResponse.success(phases, "Installment phases retrieved successfully"));
    }

    /**
     * Admin activates a construction phase — makes it DUE for payment with 15-day deadline.
     */
    @PutMapping("/{phaseId}/activate")
    public ResponseEntity<ApiResponse<InstallmentPhaseResponse>> activatePhase(
            @PathVariable Long dealId,
            @PathVariable Long phaseId) {
        InstallmentPhaseResponse phase = installmentPhaseService.activatePhase(dealId, phaseId);
        return ResponseEntity.ok(ApiResponse.success(phase,
                "Phase activated. Payment due within 15 days by " + phase.getDueDeadline()));
    }

    // Keep old endpoint for backward compat
    @PutMapping("/{phaseId}/complete")
    public ResponseEntity<ApiResponse<InstallmentPhaseResponse>> markPhaseComplete(
            @PathVariable Long dealId,
            @PathVariable Long phaseId) {
        return activatePhase(dealId, phaseId);
    }

    /**
     * Record payment against a specific construction phase.
     */
    @PostMapping("/{phaseId}/pay")
    public ResponseEntity<ApiResponse<PaymentResponse>> payPhase(
            @PathVariable Long dealId,
            @PathVariable Long phaseId,
            @Valid @RequestBody PaymentRequest request) {
        PaymentResponse payment = installmentPhaseService.payPhase(dealId, phaseId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(payment, "Phase payment recorded successfully"));
    }
}
