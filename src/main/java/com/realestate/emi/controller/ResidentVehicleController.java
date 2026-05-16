package com.realestate.emi.controller;

import com.realestate.emi.dto.request.VehicleRequest;
import com.realestate.emi.dto.response.ApiResponse;
import com.realestate.emi.dto.response.VehicleResponse;
import com.realestate.emi.service.VehicleService;
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
@RequestMapping("/api/resident/vehicles")
@RequiredArgsConstructor
@PreAuthorize("hasRole('RESIDENT')")
public class ResidentVehicleController {

    private final VehicleService vehicleService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<VehicleResponse>>> findMine() {
        return ResponseEntity.ok(ApiResponse.success(
                vehicleService.listForResident(),
                "Vehicles retrieved successfully"));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<VehicleResponse>> create(@Valid @RequestBody VehicleRequest request) {
        VehicleResponse created = vehicleService.createForResident(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(created, "Vehicle registered"));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<VehicleResponse>> update(@PathVariable Long id,
                                                               @Valid @RequestBody VehicleRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                vehicleService.updateForResident(id, request),
                "Vehicle updated"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        vehicleService.deleteForResident(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Vehicle removed"));
    }
}
