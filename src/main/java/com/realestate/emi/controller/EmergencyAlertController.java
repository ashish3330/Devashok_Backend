package com.realestate.emi.controller;

import com.realestate.emi.dto.response.ApiResponse;
import com.realestate.emi.dto.response.EmergencyAlertResponse;
import com.realestate.emi.enums.EmergencyStatus;
import com.realestate.emi.service.EmergencyAlertService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/society/emergency")
@RequiredArgsConstructor
public class EmergencyAlertController {

    private final EmergencyAlertService emergencyAlertService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','SUPERVISOR')")
    public ResponseEntity<ApiResponse<List<EmergencyAlertResponse>>> findAll(
            @RequestParam(required = false) EmergencyStatus status) {
        return ResponseEntity.ok(ApiResponse.success(
                emergencyAlertService.listForAdmin(status),
                "Emergency alerts retrieved successfully"));
    }

    @PostMapping("/{id}/acknowledge")
    @PreAuthorize("hasAnyRole('ADMIN','SUPERVISOR')")
    public ResponseEntity<ApiResponse<EmergencyAlertResponse>> acknowledge(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(
                emergencyAlertService.acknowledge(id),
                "Emergency alert acknowledged"));
    }

    @PostMapping("/{id}/resolve")
    @PreAuthorize("hasAnyRole('ADMIN','SUPERVISOR')")
    public ResponseEntity<ApiResponse<EmergencyAlertResponse>> resolve(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(
                emergencyAlertService.resolve(id),
                "Emergency alert resolved"));
    }
}
