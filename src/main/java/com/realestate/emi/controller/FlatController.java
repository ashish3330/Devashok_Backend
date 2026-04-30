package com.realestate.emi.controller;

import com.realestate.emi.dto.request.FlatRequest;
import com.realestate.emi.dto.response.ApiResponse;
import com.realestate.emi.dto.response.FlatResponse;
import com.realestate.emi.service.FlatService;
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
@RequestMapping("/api/society/flats")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','SUPERVISOR')")
public class FlatController {

    private final FlatService flatService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<FlatResponse>>> findAll(
            @RequestParam(required = false) Long blockId) {
        return ResponseEntity.ok(ApiResponse.success(flatService.findAll(blockId), "Flats retrieved successfully"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<FlatResponse>> findById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(flatService.findById(id), "Flat retrieved successfully"));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<FlatResponse>> create(@Valid @RequestBody FlatRequest request) {
        FlatResponse created = flatService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(created, "Flat created successfully"));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<FlatResponse>> update(@PathVariable Long id, @Valid @RequestBody FlatRequest request) {
        return ResponseEntity.ok(ApiResponse.success(flatService.update(id, request), "Flat updated successfully"));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        flatService.delete(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Flat deleted successfully"));
    }
}
