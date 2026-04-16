package com.realestate.emi.controller;

import com.realestate.emi.dto.request.AttendanceRequest;
import com.realestate.emi.dto.response.ApiResponse;
import com.realestate.emi.dto.response.AttendanceResponse;
import com.realestate.emi.dto.response.AttendanceSummaryResponse;
import com.realestate.emi.service.AttendanceService;
import com.realestate.emi.service.DownloadService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
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
    private final DownloadService downloadService;

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

    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<AttendanceSummaryResponse>> getAttendanceSummary(
            @RequestParam Long staffId,
            @RequestParam int month,
            @RequestParam int year) {
        return ResponseEntity.ok(ApiResponse.success(
                attendanceService.getAttendanceSummary(staffId, year, month), "Attendance summary retrieved successfully"));
    }

    @GetMapping("/summary/monthly")
    public ResponseEntity<ApiResponse<List<AttendanceSummaryResponse>>> getMonthlyAttendanceSummary(
            @RequestParam int month,
            @RequestParam int year) {
        return ResponseEntity.ok(ApiResponse.success(
                attendanceService.getMonthlyAttendanceSummary(year, month), "Monthly attendance summary retrieved successfully"));
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> exportAttendanceReport(
            @RequestParam int year,
            @RequestParam int month) {
        DownloadService.FileDownload file = downloadService.getAttendanceReport(year, month);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + file.fileName() + "\"")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(file.content());
    }
}
