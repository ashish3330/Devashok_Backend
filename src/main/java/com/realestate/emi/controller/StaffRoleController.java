package com.realestate.emi.controller;

import com.realestate.emi.dto.request.StaffRoleRequest;
import com.realestate.emi.dto.response.ApiResponse;
import com.realestate.emi.dto.response.StaffRoleResponse;
import com.realestate.emi.service.StaffRoleService;
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
@RequestMapping("/api/staff/roles")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class StaffRoleController {

    private final StaffRoleService staffRoleService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','SUPERVISOR')")
    public ResponseEntity<ApiResponse<List<StaffRoleResponse>>> findAll() {
        return ResponseEntity.ok(ApiResponse.success(staffRoleService.findAll(), "Staff roles retrieved successfully"));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<StaffRoleResponse>> create(@Valid @RequestBody StaffRoleRequest request) {
        StaffRoleResponse created = staffRoleService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(created, "Staff role created successfully"));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<StaffRoleResponse>> update(
            @PathVariable Long id, @Valid @RequestBody StaffRoleRequest request) {
        return ResponseEntity.ok(ApiResponse.success(staffRoleService.update(id, request), "Staff role updated successfully"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        staffRoleService.delete(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Staff role deleted successfully"));
    }
}
