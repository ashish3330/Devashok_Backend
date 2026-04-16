package com.realestate.emi.controller;

import com.realestate.emi.dto.response.ApiResponse;
import com.realestate.emi.dto.response.DealMaterialCostResponse;
import com.realestate.emi.dto.response.InventoryValuationResponse;
import com.realestate.emi.dto.response.StockMovementResponse;
import com.realestate.emi.dto.response.WastageReportResponse;
import com.realestate.emi.service.InventoryReportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

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

    @GetMapping("/wastage")
    public ResponseEntity<ApiResponse<WastageReportResponse>> getWastageReport(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(ApiResponse.success(
                reportService.getWastageReport(from, to), "Wastage report retrieved successfully"));
    }

    @GetMapping("/movement/{materialId}")
    public ResponseEntity<ApiResponse<StockMovementResponse>> getStockMovement(
            @PathVariable Long materialId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(ApiResponse.success(
                reportService.getStockMovementReport(materialId, from, to), "Stock movement report retrieved successfully"));
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> exportInventoryExcel() {
        byte[] excelBytes = reportService.getInventoryExcel();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentDisposition(
                ContentDisposition.attachment()
                        .filename("Inventory_Report_" + LocalDate.now() + ".xlsx", StandardCharsets.UTF_8)
                        .build());
        return ResponseEntity.ok()
                .headers(headers)
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(excelBytes);
    }
}
