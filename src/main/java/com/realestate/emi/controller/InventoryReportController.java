package com.realestate.emi.controller;

import com.realestate.emi.dto.response.ApiResponse;
import com.realestate.emi.dto.response.DealMaterialCostResponse;
import com.realestate.emi.dto.response.InventoryValuationResponse;
import com.realestate.emi.service.InventoryReportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/inventory/reports")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','SUPERVISOR')")
public class InventoryReportController {

    private final InventoryReportService reportService;

    @GetMapping("/valuation")
    public ResponseEntity<ApiResponse<InventoryValuationResponse>> getValuation() {
        return ResponseEntity.ok(ApiResponse.success(
                reportService.getInventoryValuation(), "Inventory valuation retrieved successfully"));
    }

    @GetMapping("/cost-per-deal/{dealId}")
    public ResponseEntity<ApiResponse<DealMaterialCostResponse>> getCostPerDeal(@PathVariable Long dealId) {
        return ResponseEntity.ok(ApiResponse.success(
                reportService.getCostPerDeal(dealId), "Deal material cost retrieved successfully"));
    }
}
