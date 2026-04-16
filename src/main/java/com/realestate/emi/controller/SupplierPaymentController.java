package com.realestate.emi.controller;

import com.realestate.emi.dto.request.SupplierPaymentRequest;
import com.realestate.emi.dto.response.ApiResponse;
import com.realestate.emi.dto.response.SupplierPaymentResponse;
import com.realestate.emi.dto.response.SupplierPaymentSummaryResponse;
import com.realestate.emi.service.DownloadService;
import com.realestate.emi.service.SupplierPaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/inventory/suppliers/{supplierId}/payments")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','SUPERVISOR')")
public class SupplierPaymentController {

    private final SupplierPaymentService supplierPaymentService;
    private final DownloadService downloadService;

    @PostMapping
    public ResponseEntity<ApiResponse<SupplierPaymentResponse>> recordPayment(
            @PathVariable Long supplierId,
            @Valid @RequestBody SupplierPaymentRequest request) {
        SupplierPaymentResponse payment = supplierPaymentService.recordPayment(supplierId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(payment, "Supplier payment recorded successfully"));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<SupplierPaymentResponse>>> getPayments(
            @PathVariable Long supplierId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        List<SupplierPaymentResponse> payments = supplierPaymentService.getPaymentsBySupplier(supplierId, from, to);
        return ResponseEntity.ok(ApiResponse.success(payments, "Supplier payments retrieved successfully"));
    }

    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<SupplierPaymentSummaryResponse>> getPaymentSummary(
            @PathVariable Long supplierId) {
        SupplierPaymentSummaryResponse summary = supplierPaymentService.getPaymentSummary(supplierId);
        return ResponseEntity.ok(ApiResponse.success(summary, "Supplier payment summary retrieved successfully"));
    }

    @GetMapping("/{paymentId}/receipt")
    public ResponseEntity<byte[]> downloadReceipt(
            @PathVariable Long supplierId,
            @PathVariable Long paymentId) {
        DownloadService.FileDownload file = downloadService.getSupplierPaymentReceipt(paymentId);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentDisposition(
                ContentDisposition.attachment()
                        .filename(file.fileName(), StandardCharsets.UTF_8)
                        .build());
        return ResponseEntity.ok()
                .headers(headers)
                .contentType(MediaType.APPLICATION_PDF)
                .body(file.content());
    }
}
