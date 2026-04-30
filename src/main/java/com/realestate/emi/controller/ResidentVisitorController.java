package com.realestate.emi.controller;

import com.realestate.emi.dto.request.VisitorPreApproveRequest;
import com.realestate.emi.dto.response.ApiResponse;
import com.realestate.emi.dto.response.VisitorResponse;
import com.realestate.emi.service.VisitorService;
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
@RequestMapping("/api/resident/visitors")
@RequiredArgsConstructor
@PreAuthorize("hasRole('RESIDENT')")
public class ResidentVisitorController {

    private final VisitorService visitorService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<VisitorResponse>>> findAll() {
        return ResponseEntity.ok(ApiResponse.success(visitorService.listForResident(), "Visitors retrieved successfully"));
    }

    @PostMapping("/pre-approve")
    public ResponseEntity<ApiResponse<VisitorResponse>> preApprove(@Valid @RequestBody VisitorPreApproveRequest request) {
        VisitorResponse created = visitorService.residentPreApprove(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(created, "Visitor pre-approved"));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<VisitorResponse>> cancel(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(visitorService.residentCancel(id), "Visitor cancelled"));
    }
}
