package com.realestate.emi.controller;

import com.realestate.emi.dto.request.BlockRequest;
import com.realestate.emi.dto.response.ApiResponse;
import com.realestate.emi.dto.response.BlockResponse;
import com.realestate.emi.service.BlockService;
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
@RequestMapping("/api/society/blocks")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','SUPERVISOR')")
public class BlockController {

    private final BlockService blockService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<BlockResponse>>> findAll() {
        return ResponseEntity.ok(ApiResponse.success(blockService.findAll(), "Blocks retrieved successfully"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<BlockResponse>> findById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(blockService.findById(id), "Block retrieved successfully"));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<BlockResponse>> create(@Valid @RequestBody BlockRequest request) {
        BlockResponse created = blockService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(created, "Block created successfully"));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<BlockResponse>> update(@PathVariable Long id, @Valid @RequestBody BlockRequest request) {
        return ResponseEntity.ok(ApiResponse.success(blockService.update(id, request), "Block updated successfully"));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        blockService.delete(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Block deleted successfully"));
    }
}
