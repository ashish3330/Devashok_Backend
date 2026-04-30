package com.realestate.emi.controller;

import com.realestate.emi.dto.request.ResidentPhoneRequest;
import com.realestate.emi.dto.request.ResidentRequest;
import com.realestate.emi.dto.response.ApiResponse;
import com.realestate.emi.dto.response.ResidentPhoneResponse;
import com.realestate.emi.dto.response.ResidentResponse;
import com.realestate.emi.service.ResidentService;
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
@RequestMapping("/api/society/residents")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','SUPERVISOR')")
public class ResidentManagementController {

    private final ResidentService residentService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<ResidentResponse>>> findAll(
            @RequestParam(required = false) Long blockId,
            @RequestParam(required = false) Long flatId,
            @RequestParam(required = false) Boolean active) {
        return ResponseEntity.ok(ApiResponse.success(
                residentService.findAll(blockId, flatId, active),
                "Residents retrieved successfully"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ResidentResponse>> findById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(residentService.findById(id), "Resident retrieved successfully"));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ResidentResponse>> create(@Valid @RequestBody ResidentRequest request) {
        ResidentResponse created = residentService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(created, "Resident created successfully"));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ResidentResponse>> update(@PathVariable Long id, @Valid @RequestBody ResidentRequest request) {
        return ResponseEntity.ok(ApiResponse.success(residentService.update(id, request), "Resident updated successfully"));
    }

    @PostMapping("/{id}/phones")
    public ResponseEntity<ApiResponse<ResidentPhoneResponse>> addPhone(
            @PathVariable Long id,
            @Valid @RequestBody ResidentPhoneRequest request) {
        ResidentPhoneResponse added = residentService.addPhone(id, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(added, "Phone added successfully"));
    }

    @DeleteMapping("/{id}/phones/{phoneId}")
    public ResponseEntity<ApiResponse<Void>> removePhone(@PathVariable Long id, @PathVariable Long phoneId) {
        residentService.removePhone(id, phoneId);
        return ResponseEntity.ok(ApiResponse.success(null, "Phone removed successfully"));
    }

    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<ApiResponse<Void>> deactivate(@PathVariable Long id) {
        residentService.deactivate(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Resident deactivated successfully"));
    }
}
