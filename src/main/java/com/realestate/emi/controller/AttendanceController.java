package com.realestate.emi.controller;

import com.realestate.emi.dto.request.AttendanceRequest;
import com.realestate.emi.dto.response.ApiResponse;
import com.realestate.emi.dto.response.AttendanceResponse;
import com.realestate.emi.service.AttendanceService;
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
@RequestMapping("/api/staff/attendance")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','SUPERVISOR')")
public class AttendanceController {

    private final AttendanceService attendanceService;

    @PostMapping
    public ResponseEntity<ApiResponse<AttendanceResponse>> markAttendance(
            @Valid @RequestBody AttendanceRequest request) {
        AttendanceResponse response = attendanceService.markAttendance(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Attendance marked successfully"));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<AttendanceResponse>> updateAttendance(
            @PathVariable Long id, @Valid @RequestBody AttendanceRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                attendanceService.updateAttendance(id, request), "Attendance updated successfully"));
    }

    @GetMapping("/date/{date}")
    public ResponseEntity<ApiResponse<List<AttendanceResponse>>> getByDate(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(ApiResponse.success(
                attendanceService.getByDate(date), "Attendance retrieved successfully"));
    }

    @GetMapping("/staff/{staffId}")
    public ResponseEntity<ApiResponse<List<AttendanceResponse>>> getByStaff(
            @PathVariable Long staffId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(ApiResponse.success(
                attendanceService.getByStaffAndDateRange(staffId, from, to), "Attendance retrieved successfully"));
    }
}
