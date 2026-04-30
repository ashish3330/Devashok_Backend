package com.realestate.emi.controller;

import com.realestate.emi.dto.request.ComplaintRequest;
import com.realestate.emi.dto.response.ApiResponse;
import com.realestate.emi.dto.response.ComplaintCategoryResponse;
import com.realestate.emi.dto.response.ComplaintResponse;
import com.realestate.emi.service.ComplaintCategoryService;
import com.realestate.emi.service.ComplaintService;
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
@RequestMapping("/api/resident")
@RequiredArgsConstructor
@PreAuthorize("hasRole('RESIDENT')")
public class ResidentComplaintController {

    private final ComplaintService complaintService;
    private final ComplaintCategoryService categoryService;

    @GetMapping("/complaints")
    public ResponseEntity<ApiResponse<List<ComplaintResponse>>> list() {
        return ResponseEntity.ok(ApiResponse.success(complaintService.listForResident(), "Complaints retrieved successfully"));
    }

    @GetMapping("/complaints/{id}")
    public ResponseEntity<ApiResponse<ComplaintResponse>> findById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(complaintService.findByIdForResident(id), "Complaint retrieved successfully"));
    }

    @PostMapping("/complaints")
    public ResponseEntity<ApiResponse<ComplaintResponse>> raise(@Valid @RequestBody ComplaintRequest request) {
        ComplaintResponse created = complaintService.residentRaise(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(created, "Complaint raised"));
    }

    @GetMapping("/complaint-categories")
    public ResponseEntity<ApiResponse<List<ComplaintCategoryResponse>>> categories() {
        return ResponseEntity.ok(ApiResponse.success(categoryService.findAll(), "Categories retrieved successfully"));
    }
}
