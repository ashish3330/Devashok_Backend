package com.realestate.emi.controller;

import com.realestate.emi.dto.request.DealRequest;
import com.realestate.emi.dto.request.DealStatusRequest;
import com.realestate.emi.dto.response.ApiResponse;
import com.realestate.emi.dto.response.DealDetailResponse;
import com.realestate.emi.dto.response.DealSummaryResponse;
import com.realestate.emi.service.DealService;
import com.realestate.emi.service.DownloadService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/deals")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class DealController {

    private final DealService dealService;
    private final DownloadService downloadService;

    @PostMapping
    public ResponseEntity<ApiResponse<DealDetailResponse>> createDeal(
            @Valid @RequestBody DealRequest request) {
        DealDetailResponse deal = dealService.createDeal(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(deal, "Deal created successfully"));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<DealSummaryResponse>>> findAll() {
        List<DealSummaryResponse> deals = dealService.findAll();
        return ResponseEntity.ok(ApiResponse.success(deals, "Deals retrieved successfully"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<DealDetailResponse>> findById(@PathVariable Long id) {
        DealDetailResponse deal = dealService.findById(id);
        return ResponseEntity.ok(ApiResponse.success(deal, "Deal retrieved successfully"));
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<ApiResponse<DealDetailResponse>> changeStatus(
            @PathVariable Long id,
            @Valid @RequestBody DealStatusRequest request) {
        DealDetailResponse deal = dealService.changeStatus(id, request);
        return ResponseEntity.ok(ApiResponse.success(deal, "Deal status updated successfully"));
    }

    @GetMapping("/{id}/schedule/download")
    public ResponseEntity<byte[]> downloadSchedule(@PathVariable Long id) {
        DownloadService.FileDownload file = downloadService.getEmiScheduleExcel(id);
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
}
