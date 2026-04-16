package com.realestate.emi.controller;

import com.realestate.emi.dto.request.StaffRequest;
import com.realestate.emi.dto.response.ApiResponse;
import com.realestate.emi.dto.response.StaffResponse;
import com.realestate.emi.service.StaffService;
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
@RequestMapping("/api/staff")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','SUPERVISOR')")
public class StaffController {

    private final StaffService staffService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<StaffResponse>>> findAll() {
        return ResponseEntity.ok(ApiResponse.success(staffService.findAll(), "Staff retrieved successfully"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<StaffResponse>> findById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(staffService.findById(id), "Staff retrieved successfully"));
    }

    @GetMapping("/role/{roleId}")
    public ResponseEntity<ApiResponse<List<StaffResponse>>> findByRole(@PathVariable Long roleId) {
        return ResponseEntity.ok(ApiResponse.success(staffService.findByRole(roleId), "Staff retrieved successfully"));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<StaffResponse>> create(@Valid @RequestBody StaffRequest request) {
        StaffResponse created = staffService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(created, "Staff created successfully"));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<StaffResponse>> update(
            @PathVariable Long id, @Valid @RequestBody StaffRequest request) {
        return ResponseEntity.ok(ApiResponse.success(staffService.update(id, request), "Staff updated successfully"));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deactivate(@PathVariable Long id) {
        staffService.deactivate(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Staff deactivated successfully"));
    }
}
