package com.realestate.emi.controller;

import com.realestate.emi.dto.response.ApiResponse;
import com.realestate.emi.dto.response.VisitorResponse;
import com.realestate.emi.enums.VisitorStatus;
import com.realestate.emi.service.VisitorService;
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
@RequestMapping("/api/society/visitors")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','SUPERVISOR')")
public class VisitorController {

    private final VisitorService visitorService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<VisitorResponse>>> findAll(
            @RequestParam(required = false) Long blockId,
            @RequestParam(required = false) Long flatId,
            @RequestParam(required = false) VisitorStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(ApiResponse.success(
                visitorService.listAdmin(blockId, flatId, status, from, to),
                "Visitors retrieved successfully"));
    }

    @PostMapping("/{id}/check-in")
    public ResponseEntity<ApiResponse<VisitorResponse>> checkIn(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(visitorService.adminCheckIn(id), "Visitor checked in"));
    }

    @PostMapping("/{id}/check-out")
    public ResponseEntity<ApiResponse<VisitorResponse>> checkOut(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(visitorService.adminCheckOut(id), "Visitor checked out"));
    }
}
