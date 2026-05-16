package com.realestate.emi.controller;

import com.realestate.emi.dto.request.FamilyMemberRequest;
import com.realestate.emi.dto.response.ApiResponse;
import com.realestate.emi.dto.response.FamilyMemberResponse;
import com.realestate.emi.service.FamilyMemberService;
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
@RequestMapping("/api/resident/family")
@RequiredArgsConstructor
@PreAuthorize("hasRole('RESIDENT')")
public class ResidentFamilyMemberController {

    private final FamilyMemberService familyMemberService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<FamilyMemberResponse>>> findAll() {
        return ResponseEntity.ok(ApiResponse.success(
                familyMemberService.listForResident(),
                "Family members retrieved successfully"));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<FamilyMemberResponse>> create(@Valid @RequestBody FamilyMemberRequest request) {
        FamilyMemberResponse created = familyMemberService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(created, "Family member added"));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<FamilyMemberResponse>> update(@PathVariable Long id,
                                                                    @Valid @RequestBody FamilyMemberRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                familyMemberService.update(id, request),
                "Family member updated"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        familyMemberService.delete(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Family member removed"));
    }
}
