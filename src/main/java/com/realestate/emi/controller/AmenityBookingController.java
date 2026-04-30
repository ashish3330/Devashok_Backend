package com.realestate.emi.controller;

import com.realestate.emi.dto.response.AmenityBookingResponse;
import com.realestate.emi.dto.response.ApiResponse;
import com.realestate.emi.enums.AmenityBookingStatus;
import com.realestate.emi.service.AmenityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/society/amenity-bookings")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','SUPERVISOR')")
public class AmenityBookingController {

    private final AmenityService amenityService;

    @GetMapping("/availability")
    public ResponseEntity<ApiResponse<List<AmenityBookingResponse>>> availability(
            @RequestParam Long amenityId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(ApiResponse.success(
                amenityService.availability(amenityId, date), "Availability retrieved"));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<AmenityBookingResponse>> updateStatus(
            @PathVariable Long id,
            @RequestParam AmenityBookingStatus status) {
        return ResponseEntity.ok(ApiResponse.success(
                amenityService.adminUpdateStatus(id, status), "Booking status updated"));
    }
}
