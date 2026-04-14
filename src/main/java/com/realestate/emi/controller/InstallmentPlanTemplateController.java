package com.realestate.emi.controller;

import com.realestate.emi.dto.request.InstallmentPlanTemplateRequest;
import com.realestate.emi.dto.response.ApiResponse;
import com.realestate.emi.dto.response.InstallmentPlanTemplateResponse;
import com.realestate.emi.entity.InstallmentPlanTemplate;
import com.realestate.emi.mapper.InstallmentPhaseMapper;
import com.realestate.emi.repository.InstallmentPlanTemplateRepository;
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
@RequestMapping("/api/installment-templates")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class InstallmentPlanTemplateController {

    private final InstallmentPlanTemplateRepository templateRepository;
    private final InstallmentPhaseMapper mapper;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','VIEWER')")
    public ResponseEntity<ApiResponse<List<InstallmentPlanTemplateResponse>>> findAll() {
        List<InstallmentPlanTemplate> templates = templateRepository.findByIsActiveTrueOrderByPhaseOrderAsc();
        return ResponseEntity.ok(ApiResponse.success(
                mapper.toTemplateResponseList(templates), "Installment templates retrieved successfully"));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<InstallmentPlanTemplateResponse>> create(
            @Valid @RequestBody InstallmentPlanTemplateRequest request) {
        InstallmentPlanTemplate template = InstallmentPlanTemplate.builder()
                .phaseName(request.getPhaseName())
                .phaseOrder(request.getPhaseOrder())
                .percentageOfTotal(request.getPercentageOfTotal())
                .description(request.getDescription())
                .isActive(true)
                .build();
        template = templateRepository.save(template);
        log.info("Created installment template: {}", template.getPhaseName());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(mapper.toTemplateResponse(template), "Template created successfully"));
    }
}
