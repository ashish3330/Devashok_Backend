package com.realestate.emi.controller;

import com.realestate.emi.dto.response.ApiResponse;
import com.realestate.emi.dto.response.NoticeResponse;
import com.realestate.emi.service.NoticeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/resident/notices")
@RequiredArgsConstructor
@PreAuthorize("hasRole('RESIDENT')")
public class ResidentNoticeController {

    private final NoticeService noticeService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<NoticeResponse>>> feed() {
        return ResponseEntity.ok(ApiResponse.success(noticeService.findResidentFeed(), "Notices retrieved successfully"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<NoticeResponse>> findById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(noticeService.findResidentById(id), "Notice retrieved successfully"));
    }
}
