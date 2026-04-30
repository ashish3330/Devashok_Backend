package com.realestate.emi.controller;

import com.realestate.emi.dto.request.AmenityRequest;
import com.realestate.emi.dto.response.AmenityResponse;
import com.realestate.emi.dto.response.ApiResponse;
import com.realestate.emi.service.AmenityService;
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
@RequestMapping("/api/society/amenities")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','SUPERVISOR')")
public class AmenityController {

    private final AmenityService amenityService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<AmenityResponse>>> findAll() {
        return ResponseEntity.ok(ApiResponse.success(amenityService.findAllAdmin(), "Amenities retrieved successfully"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<AmenityResponse>> findById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(amenityService.findById(id), "Amenity retrieved successfully"));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<AmenityResponse>> create(@Valid @RequestBody AmenityRequest request) {
        AmenityResponse created = amenityService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(created, "Amenity created successfully"));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<AmenityResponse>> update(@PathVariable Long id, @Valid @RequestBody AmenityRequest request) {
        return ResponseEntity.ok(ApiResponse.success(amenityService.update(id, request), "Amenity updated successfully"));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        amenityService.delete(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Amenity deleted successfully"));
    }
}
