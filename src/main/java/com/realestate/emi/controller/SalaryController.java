package com.realestate.emi.controller;

import com.realestate.emi.dto.request.SalaryPaymentRequest;
import com.realestate.emi.dto.response.ApiResponse;
import com.realestate.emi.dto.response.SalaryRecordResponse;
import com.realestate.emi.entity.SalaryRecord;
import com.realestate.emi.service.DownloadService;
import com.realestate.emi.service.SalaryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/staff/salary")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','SUPERVISOR')")
public class SalaryController {

    private final SalaryService salaryService;
    private final DownloadService downloadService;

    @PostMapping("/generate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<SalaryRecordResponse>>> generateSalary(
            @RequestParam int year, @RequestParam int month) {
        List<SalaryRecordResponse> records = salaryService.generateMonthlySalary(year, month);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(records, "Salary records generated successfully"));
    }

    @PostMapping("/{salaryId}/pay")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<SalaryRecordResponse>> paySalary(
            @PathVariable Long salaryId,
            @Valid @RequestBody SalaryPaymentRequest request) {
        SalaryRecordResponse response = salaryService.paySalary(salaryId, request);
        return ResponseEntity.ok(ApiResponse.success(response, "Salary payment recorded successfully"));
    }

    @GetMapping("/monthly")
    public ResponseEntity<ApiResponse<List<SalaryRecordResponse>>> getSalaryByMonth(
            @RequestParam int year, @RequestParam int month) {
        return ResponseEntity.ok(ApiResponse.success(
                salaryService.getSalaryByMonth(year, month), "Salary records retrieved successfully"));
    }

    @GetMapping("/staff/{staffId}")
    public ResponseEntity<ApiResponse<List<SalaryRecordResponse>>> getSalaryByStaff(@PathVariable Long staffId) {
        return ResponseEntity.ok(ApiResponse.success(
                salaryService.getSalaryByStaff(staffId), "Salary records retrieved successfully"));
    }

    @GetMapping("/pending")
    public ResponseEntity<ApiResponse<List<SalaryRecordResponse>>> getPendingSalaries() {
        return ResponseEntity.ok(ApiResponse.success(
                salaryService.getPendingSalaries(), "Pending salary records retrieved successfully"));
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> exportPayroll(@RequestParam int year, @RequestParam int month) {
        List<SalaryRecord> records = salaryService.getSalaryRecordsByMonth(year, month);
        DownloadService.FileDownload file = downloadService.getPayrollExcel(year, month, records);
        MediaType xlsx = MediaType.parseMediaType(
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        HttpHeaders headers = new HttpHeaders();
        headers.setContentDisposition(
                ContentDisposition.attachment()
                        .filename(file.fileName(), StandardCharsets.UTF_8)
                        .build());
        return ResponseEntity.ok()
                .headers(headers)
                .contentType(xlsx)
                .body(file.content());
    }

    @GetMapping("/{salaryId}/slip")
    public ResponseEntity<byte[]> downloadSalarySlip(@PathVariable Long salaryId) {
        DownloadService.FileDownload file = downloadService.getSalarySlip(salaryId);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentDisposition(
                ContentDisposition.attachment()
                        .filename(file.fileName(), StandardCharsets.UTF_8)
                        .build());
        return ResponseEntity.ok()
                .headers(headers)
                .contentType(MediaType.APPLICATION_PDF)
                .body(file.content());
    }
}
