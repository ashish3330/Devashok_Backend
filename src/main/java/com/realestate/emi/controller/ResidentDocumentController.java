package com.realestate.emi.controller;

import com.realestate.emi.dto.request.ResidentDocumentRequest;
import com.realestate.emi.dto.response.ApiResponse;
import com.realestate.emi.dto.response.ResidentDocumentResponse;
import com.realestate.emi.service.ResidentDocumentService;
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
@RequestMapping("/api/society/documents")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','SUPERVISOR')")
public class ResidentDocumentController {

    private final ResidentDocumentService documentService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<ResidentDocumentResponse>>> findAll() {
        return ResponseEntity.ok(ApiResponse.success(documentService.findAllAdmin(), "Documents retrieved successfully"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ResidentDocumentResponse>> findById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(documentService.findById(id), "Document retrieved successfully"));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ResidentDocumentResponse>> create(@Valid @RequestBody ResidentDocumentRequest request) {
        ResidentDocumentResponse created = documentService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(created, "Document created successfully"));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ResidentDocumentResponse>> update(@PathVariable Long id, @Valid @RequestBody ResidentDocumentRequest request) {
        return ResponseEntity.ok(ApiResponse.success(documentService.update(id, request), "Document updated successfully"));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        documentService.delete(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Document deleted successfully"));
    }
}
