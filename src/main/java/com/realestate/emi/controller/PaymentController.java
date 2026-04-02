package com.realestate.emi.controller;

import com.realestate.emi.dto.request.PaymentRequest;
import com.realestate.emi.dto.response.ApiResponse;
import com.realestate.emi.dto.response.PaymentResponse;
import com.realestate.emi.service.PaymentService;
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
@RequestMapping("/api/deals/{dealId}/payments")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping
    public ResponseEntity<ApiResponse<PaymentResponse>> recordPayment(
            @PathVariable Long dealId,
            @Valid @RequestBody PaymentRequest request) {
        PaymentResponse payment = paymentService.recordPayment(dealId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(payment, "Payment recorded successfully"));
    }

    @PostMapping("/emi-schedules/{scheduleId}/pay")
    public ResponseEntity<ApiResponse<PaymentResponse>> paySpecificEmi(
            @PathVariable Long dealId,
            @PathVariable Long scheduleId,
            @Valid @RequestBody PaymentRequest request) {
        PaymentResponse payment = paymentService.paySpecificEmi(dealId, scheduleId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(payment, "EMI payment recorded successfully"));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<PaymentResponse>>> getPaymentsByDeal(
            @PathVariable Long dealId) {
        List<PaymentResponse> payments = paymentService.getPaymentsByDeal(dealId);
        return ResponseEntity.ok(ApiResponse.success(payments, "Payments retrieved successfully"));
    }
}
