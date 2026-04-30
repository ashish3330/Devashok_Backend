package com.realestate.emi.controller;

import com.realestate.emi.dto.response.ApiResponse;
import com.realestate.emi.dto.response.ResidentDocumentResponse;
import com.realestate.emi.service.ResidentDocumentService;
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
@RequestMapping("/api/resident/documents")
@RequiredArgsConstructor
@PreAuthorize("hasRole('RESIDENT')")
public class ResidentDocumentResidentController {

    private final ResidentDocumentService documentService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<ResidentDocumentResponse>>> feed() {
        return ResponseEntity.ok(ApiResponse.success(documentService.findResidentFeed(), "Documents retrieved successfully"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ResidentDocumentResponse>> findById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(documentService.findByIdForResident(id), "Document retrieved successfully"));
    }
}
