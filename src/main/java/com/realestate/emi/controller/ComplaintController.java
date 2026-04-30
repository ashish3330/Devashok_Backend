package com.realestate.emi.controller;

import com.realestate.emi.dto.request.ComplaintStatusUpdateRequest;
import com.realestate.emi.dto.response.ApiResponse;
import com.realestate.emi.dto.response.ComplaintResponse;
import com.realestate.emi.enums.ComplaintStatus;
import com.realestate.emi.service.ComplaintService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/society/complaints")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','SUPERVISOR')")
public class ComplaintController {

    private final ComplaintService complaintService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<ComplaintResponse>>> findAll(
            @RequestParam(required = false) Long blockId,
            @RequestParam(required = false) Long flatId,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) ComplaintStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(ApiResponse.success(
                complaintService.listAdmin(blockId, flatId, categoryId, status, from, to),
                "Complaints retrieved successfully"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ComplaintResponse>> findById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(complaintService.findByIdAdmin(id), "Complaint retrieved successfully"));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<ComplaintResponse>> updateStatus(@PathVariable Long id,
                                                                       @Valid @RequestBody ComplaintStatusUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.success(complaintService.updateStatus(id, request), "Status updated"));
    }
}
