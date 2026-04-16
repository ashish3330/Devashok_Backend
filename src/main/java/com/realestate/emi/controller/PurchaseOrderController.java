package com.realestate.emi.controller;

import com.realestate.emi.dto.request.PurchaseOrderReceiveRequest;
import com.realestate.emi.dto.request.PurchaseOrderRequest;
import com.realestate.emi.dto.response.ApiResponse;
import com.realestate.emi.dto.response.PurchaseOrderResponse;
import com.realestate.emi.enums.PurchaseOrderStatus;
import com.realestate.emi.service.PurchaseOrderService;
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
@RequestMapping("/api/inventory/purchase-orders")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','SUPERVISOR')")
public class PurchaseOrderController {

    private final PurchaseOrderService poService;

    @PostMapping
    public ResponseEntity<ApiResponse<PurchaseOrderResponse>> create(
            @Valid @RequestBody PurchaseOrderRequest request) {
        PurchaseOrderResponse response = poService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Purchase order created successfully"));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<PurchaseOrderResponse>>> findAll(
            @RequestParam(required = false) PurchaseOrderStatus status) {
        return ResponseEntity.ok(ApiResponse.success(
                poService.findAll(status), "Purchase orders retrieved successfully"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<PurchaseOrderResponse>> findById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(
                poService.findById(id), "Purchase order retrieved successfully"));
    }

    @GetMapping("/supplier/{supplierId}")
    public ResponseEntity<ApiResponse<List<PurchaseOrderResponse>>> findBySupplier(
            @PathVariable Long supplierId) {
        return ResponseEntity.ok(ApiResponse.success(
                poService.findBySupplier(supplierId), "Purchase orders retrieved successfully"));
    }

    @PutMapping("/{id}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<PurchaseOrderResponse>> approve(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(
                poService.approve(id), "Purchase order approved successfully"));
    }

    @PutMapping("/{id}/receive")
    public ResponseEntity<ApiResponse<PurchaseOrderResponse>> receiveItems(
            @PathVariable Long id,
            @Valid @RequestBody PurchaseOrderReceiveRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                poService.receiveItems(id, request), "Items received successfully"));
    }

    @PutMapping("/{id}/cancel")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<PurchaseOrderResponse>> cancel(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(
                poService.cancel(id), "Purchase order cancelled successfully"));
    }
}
