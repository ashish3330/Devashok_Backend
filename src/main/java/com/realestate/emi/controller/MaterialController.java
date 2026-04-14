package com.realestate.emi.controller;

import com.realestate.emi.dto.request.MaterialRequest;
import com.realestate.emi.dto.response.ApiResponse;
import com.realestate.emi.dto.response.MaterialResponse;
import com.realestate.emi.dto.response.StockAlertResponse;
import com.realestate.emi.enums.MaterialCategory;
import com.realestate.emi.service.MaterialService;
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
@RequestMapping("/api/inventory/materials")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','SUPERVISOR')")
public class MaterialController {

    private final MaterialService materialService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<MaterialResponse>>> findAll() {
        return ResponseEntity.ok(ApiResponse.success(materialService.findAll(), "Materials retrieved successfully"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<MaterialResponse>> findById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(materialService.findById(id), "Material retrieved successfully"));
    }

    @GetMapping("/category/{category}")
    public ResponseEntity<ApiResponse<List<MaterialResponse>>> findByCategory(@PathVariable MaterialCategory category) {
        return ResponseEntity.ok(ApiResponse.success(materialService.findByCategory(category), "Materials retrieved successfully"));
    }

    @GetMapping("/low-stock")
    public ResponseEntity<ApiResponse<List<MaterialResponse>>> findLowStock() {
        return ResponseEntity.ok(ApiResponse.success(materialService.findLowStock(), "Low stock materials retrieved successfully"));
    }

    @GetMapping("/alerts")
    public ResponseEntity<ApiResponse<List<StockAlertResponse>>> getAlerts() {
        return ResponseEntity.ok(ApiResponse.success(
                materialService.getUnacknowledgedAlerts(), "Alerts retrieved successfully"));
    }

    @PutMapping("/alerts/{alertId}/acknowledge")
    public ResponseEntity<ApiResponse<Void>> acknowledgeAlert(@PathVariable Long alertId) {
        materialService.acknowledgeAlert(alertId, "admin");
        return ResponseEntity.ok(ApiResponse.success(null, "Alert acknowledged successfully"));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<MaterialResponse>> create(@Valid @RequestBody MaterialRequest request) {
        MaterialResponse created = materialService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(created, "Material created successfully"));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<MaterialResponse>> update(
            @PathVariable Long id, @Valid @RequestBody MaterialRequest request) {
        return ResponseEntity.ok(ApiResponse.success(materialService.update(id, request), "Material updated successfully"));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        materialService.delete(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Material deleted successfully"));
    }
}
