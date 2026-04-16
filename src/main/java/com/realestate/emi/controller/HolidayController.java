package com.realestate.emi.controller;

import com.realestate.emi.dto.request.HolidayRequest;
import com.realestate.emi.dto.response.ApiResponse;
import com.realestate.emi.dto.response.HolidayResponse;
import com.realestate.emi.service.HolidayService;
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
@RequestMapping("/api/staff/holidays")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class HolidayController {

    private final HolidayService holidayService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','SUPERVISOR')")
    public ResponseEntity<ApiResponse<List<HolidayResponse>>> findAll(
            @RequestParam(required = false) Integer year) {
        List<HolidayResponse> holidays = year != null
                ? holidayService.findByYear(year)
                : holidayService.findAll();
        return ResponseEntity.ok(ApiResponse.success(holidays, "Holidays retrieved successfully"));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<HolidayResponse>> create(@Valid @RequestBody HolidayRequest request) {
        HolidayResponse created = holidayService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(created, "Holiday created successfully"));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<HolidayResponse>> update(
            @PathVariable Long id, @Valid @RequestBody HolidayRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                holidayService.update(id, request), "Holiday updated successfully"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        holidayService.delete(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Holiday deleted successfully"));
    }
}
