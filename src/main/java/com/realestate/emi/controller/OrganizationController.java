package com.realestate.emi.controller;

import com.realestate.emi.dto.response.ApiResponse;
import com.realestate.emi.entity.Organization;
import com.realestate.emi.exception.ResourceNotFoundException;
import com.realestate.emi.repository.OrganizationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/organizations")
@RequiredArgsConstructor
public class OrganizationController {

    private final OrganizationRepository organizationRepository;

    // PUBLIC - no auth needed, used by login page
    @GetMapping("/public/{code}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getByCode(@PathVariable String code) {
        Organization org = organizationRepository.findByCode(code.toUpperCase())
                .orElseThrow(() -> new ResourceNotFoundException("Organization with code '" + code + "' not found"));

        Map<String, Object> data = Map.of(
            "id", org.getId(),
            "name", org.getName(),
            "code", org.getCode(),
            "address", org.getAddress() != null ? org.getAddress() : "",
            "phone", org.getPhone() != null ? org.getPhone() : "",
            "email", org.getEmail() != null ? org.getEmail() : "",
            "reraNumber", org.getReraNumber() != null ? org.getReraNumber() : "",
            "logoUrl", org.getLogoUrl() != null ? org.getLogoUrl() : ""
        );
        return ResponseEntity.ok(ApiResponse.success(data, "Organization found"));
    }

    // Protected - admin can manage their own org
    @GetMapping("/me")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getMyOrg() {
        // This will be filtered by TenantContext once available
        return ResponseEntity.ok(ApiResponse.success(null, "OK"));
    }
}
