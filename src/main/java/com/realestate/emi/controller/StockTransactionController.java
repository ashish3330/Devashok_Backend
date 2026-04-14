package com.realestate.emi.controller;

import com.realestate.emi.dto.request.StockTransactionRequest;
import com.realestate.emi.dto.response.ApiResponse;
import com.realestate.emi.dto.response.StockTransactionResponse;
import com.realestate.emi.service.StockTransactionService;
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
@RequestMapping("/api/inventory/transactions")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','SUPERVISOR')")
public class StockTransactionController {

    private final StockTransactionService transactionService;

    @PostMapping
    public ResponseEntity<ApiResponse<StockTransactionResponse>> recordTransaction(
            @Valid @RequestBody StockTransactionRequest request) {
        StockTransactionResponse response = transactionService.recordTransaction(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Transaction recorded successfully"));
    }

    @GetMapping("/material/{materialId}")
    public ResponseEntity<ApiResponse<List<StockTransactionResponse>>> getByMaterial(@PathVariable Long materialId) {
        return ResponseEntity.ok(ApiResponse.success(
                transactionService.getByMaterial(materialId), "Transactions retrieved successfully"));
    }

    @GetMapping("/deal/{dealId}")
    public ResponseEntity<ApiResponse<List<StockTransactionResponse>>> getByDeal(@PathVariable Long dealId) {
        return ResponseEntity.ok(ApiResponse.success(
                transactionService.getByDeal(dealId), "Transactions retrieved successfully"));
    }
}
