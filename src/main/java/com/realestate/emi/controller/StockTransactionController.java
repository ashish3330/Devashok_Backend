package com.realestate.emi.controller;

import com.realestate.emi.dto.request.StockTransactionRequest;
import com.realestate.emi.dto.response.ApiResponse;
import com.realestate.emi.dto.response.StockTransactionResponse;
import com.realestate.emi.service.DownloadService;
import com.realestate.emi.service.StockTransactionService;
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
@RequestMapping("/api/inventory/transactions")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','SUPERVISOR')")
public class StockTransactionController {

    private final StockTransactionService transactionService;
    private final DownloadService downloadService;

    @PostMapping
    public ResponseEntity<ApiResponse<StockTransactionResponse>> recordTransaction(
            @Valid @RequestBody StockTransactionRequest request) {
        StockTransactionResponse response = transactionService.recordTransaction(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Transaction recorded successfully"));
    }

    @GetMapping("/material/{materialId}")
    public ResponseEntity<ApiResponse<List<StockTransactionResponse>>> getByMaterial(
            @PathVariable Long materialId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(ApiResponse.success(
                transactionService.getByMaterial(materialId, from, to), "Transactions retrieved successfully"));
    }

    @GetMapping("/deal/{dealId}")
    public ResponseEntity<ApiResponse<List<StockTransactionResponse>>> getByDeal(@PathVariable Long dealId) {
        return ResponseEntity.ok(ApiResponse.success(
                transactionService.getByDeal(dealId), "Transactions retrieved successfully"));
    }

    @GetMapping("/supplier/{supplierId}")
    public ResponseEntity<ApiResponse<List<StockTransactionResponse>>> getBySupplier(
            @PathVariable Long supplierId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(ApiResponse.success(
                transactionService.getBySupplier(supplierId, from, to), "Transactions retrieved successfully"));
    }

    @GetMapping("/{transactionId}/receipt")
    public ResponseEntity<byte[]> downloadReceipt(@PathVariable Long transactionId) {
        DownloadService.FileDownload file = downloadService.getStockTransactionReceipt(transactionId);
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
