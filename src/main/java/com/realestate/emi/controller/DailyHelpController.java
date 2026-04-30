package com.realestate.emi.controller;

import com.realestate.emi.dto.request.DailyHelpRequest;
import com.realestate.emi.dto.response.ApiResponse;
import com.realestate.emi.dto.response.DailyHelpAttendanceResponse;
import com.realestate.emi.dto.response.DailyHelpResponse;
import com.realestate.emi.service.DailyHelpService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/society/daily-help")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','SUPERVISOR')")
public class DailyHelpController {

    private final DailyHelpService dailyHelpService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<DailyHelpResponse>>> findAll() {
        return ResponseEntity.ok(ApiResponse.success(dailyHelpService.findAll(), "Daily helps retrieved"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<DailyHelpResponse>> findById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(dailyHelpService.findById(id), "Daily help retrieved"));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<DailyHelpResponse>> create(@Valid @RequestBody DailyHelpRequest request) {
        DailyHelpResponse created = dailyHelpService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(created, "Daily help created"));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<DailyHelpResponse>> update(@PathVariable Long id, @Valid @RequestBody DailyHelpRequest request) {
        return ResponseEntity.ok(ApiResponse.success(dailyHelpService.update(id, request), "Daily help updated"));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        dailyHelpService.delete(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Daily help deleted"));
    }

    @PostMapping("/attendance/{helpId}/check-in")
    public ResponseEntity<ApiResponse<DailyHelpAttendanceResponse>> checkIn(
            @PathVariable Long helpId, @RequestParam Long flatId) {
        return ResponseEntity.ok(ApiResponse.success(dailyHelpService.adminCheckIn(helpId, flatId), "Checked in"));
    }

    @PostMapping("/attendance/{attendanceId}/check-out")
    public ResponseEntity<ApiResponse<DailyHelpAttendanceResponse>> checkOut(@PathVariable Long attendanceId) {
        return ResponseEntity.ok(ApiResponse.success(dailyHelpService.adminCheckOut(attendanceId), "Checked out"));
    }

    @GetMapping("/attendance")
    public ResponseEntity<ApiResponse<List<DailyHelpAttendanceResponse>>> attendance(
            @RequestParam Long helpId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(ApiResponse.success(dailyHelpService.adminAttendance(helpId, from, to), "Attendance retrieved"));
    }
}
