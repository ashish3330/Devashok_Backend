package com.realestate.emi.controller;

import com.realestate.emi.dto.request.AmenityBookingRequest;
import com.realestate.emi.dto.response.AmenityBookingResponse;
import com.realestate.emi.dto.response.AmenityResponse;
import com.realestate.emi.dto.response.ApiResponse;
import com.realestate.emi.service.AmenityService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/resident/amenities")
@RequiredArgsConstructor
@PreAuthorize("hasRole('RESIDENT')")
public class ResidentAmenityController {

    private final AmenityService amenityService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<AmenityResponse>>> list() {
        return ResponseEntity.ok(ApiResponse.success(amenityService.findActiveForResident(), "Amenities retrieved"));
    }

    @GetMapping("/{id}/availability")
    public ResponseEntity<ApiResponse<List<AmenityBookingResponse>>> availability(
            @PathVariable Long id,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(ApiResponse.success(amenityService.availability(id, date), "Availability retrieved"));
    }

    @PostMapping("/{id}/book")
    public ResponseEntity<ApiResponse<AmenityBookingResponse>> book(
            @PathVariable Long id, @Valid @RequestBody AmenityBookingRequest request) {
        AmenityBookingResponse booking = amenityService.residentBook(id, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(booking, "Booking created"));
    }

    @GetMapping("/bookings/me")
    public ResponseEntity<ApiResponse<List<AmenityBookingResponse>>> myBookings() {
        return ResponseEntity.ok(ApiResponse.success(amenityService.myBookings(), "Bookings retrieved"));
    }

    @PostMapping("/bookings/{id}/cancel")
    public ResponseEntity<ApiResponse<AmenityBookingResponse>> cancel(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(amenityService.residentCancel(id), "Booking cancelled"));
    }
}
