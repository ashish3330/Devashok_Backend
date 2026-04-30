package com.realestate.emi.controller;

import com.realestate.emi.dto.request.DailyHelpAssignmentRequest;
import com.realestate.emi.dto.response.ApiResponse;
import com.realestate.emi.dto.response.DailyHelpAssignmentResponse;
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
@RequestMapping("/api/resident/daily-help")
@RequiredArgsConstructor
@PreAuthorize("hasRole('RESIDENT')")
public class ResidentDailyHelpController {

    private final DailyHelpService dailyHelpService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<DailyHelpAssignmentResponse>>> myHelpers() {
        return ResponseEntity.ok(ApiResponse.success(dailyHelpService.residentAssignments(), "Helpers retrieved"));
    }

    @GetMapping("/directory")
    public ResponseEntity<ApiResponse<List<DailyHelpResponse>>> directory() {
        return ResponseEntity.ok(ApiResponse.success(dailyHelpService.findAll(), "Directory retrieved"));
    }

    @PostMapping("/assignments")
    public ResponseEntity<ApiResponse<DailyHelpAssignmentResponse>> assign(
            @Valid @RequestBody DailyHelpAssignmentRequest request) {
        DailyHelpAssignmentResponse assignment = dailyHelpService.residentAssign(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(assignment, "Helper assigned"));
    }

    @DeleteMapping("/assignments/{id}")
    public ResponseEntity<ApiResponse<Void>> unassign(@PathVariable Long id) {
        dailyHelpService.residentUnassign(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Helper unassigned"));
    }

    @GetMapping("/attendance")
    public ResponseEntity<ApiResponse<List<DailyHelpAttendanceResponse>>> attendance(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(ApiResponse.success(dailyHelpService.residentAttendance(from, to), "Attendance retrieved"));
    }
}
