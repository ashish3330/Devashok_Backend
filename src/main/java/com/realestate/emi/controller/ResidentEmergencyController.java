package com.realestate.emi.controller;

import com.realestate.emi.dto.request.EmergencyAlertRequest;
import com.realestate.emi.dto.response.ApiResponse;
import com.realestate.emi.dto.response.EmergencyAlertResponse;
import com.realestate.emi.service.EmergencyAlertService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/resident/emergency")
@RequiredArgsConstructor
@PreAuthorize("hasRole('RESIDENT')")
public class ResidentEmergencyController {

    private final EmergencyAlertService emergencyAlertService;

    @PostMapping("/raise")
    public ResponseEntity<ApiResponse<EmergencyAlertResponse>> raise(@Valid @RequestBody EmergencyAlertRequest request) {
        EmergencyAlertResponse response = emergencyAlertService.raise(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Emergency alert raised"));
    }

    @GetMapping("/mine")
    public ResponseEntity<ApiResponse<List<EmergencyAlertResponse>>> mine() {
        return ResponseEntity.ok(ApiResponse.success(emergencyAlertService.listMine(), "Alerts retrieved successfully"));
    }
}
