package com.realestate.emi.controller;

import com.realestate.emi.dto.request.PropertyTypeRequest;
import com.realestate.emi.dto.response.ApiResponse;
import com.realestate.emi.dto.response.PropertyTypeResponse;
import com.realestate.emi.service.PropertyTypeService;
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
@RequestMapping("/api/property-types")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class PropertyTypeController {

    private final PropertyTypeService propertyTypeService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','VIEWER')")
    public ResponseEntity<ApiResponse<List<PropertyTypeResponse>>> findAll() {
        List<PropertyTypeResponse> propertyTypes = propertyTypeService.findAll();
        return ResponseEntity.ok(ApiResponse.success(propertyTypes, "Property types retrieved successfully"));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<PropertyTypeResponse>> create(
            @Valid @RequestBody PropertyTypeRequest request) {
        PropertyTypeResponse created = propertyTypeService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(created, "Property type created successfully"));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<PropertyTypeResponse>> update(
            @PathVariable Long id,
            @Valid @RequestBody PropertyTypeRequest request) {
        PropertyTypeResponse updated = propertyTypeService.update(id, request);
        return ResponseEntity.ok(ApiResponse.success(updated, "Property type updated successfully"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        propertyTypeService.delete(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Property type deleted successfully"));
    }
}
