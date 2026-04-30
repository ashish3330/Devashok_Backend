package com.realestate.emi.controller;

import com.realestate.emi.dto.request.ResidentOtpRequest;
import com.realestate.emi.dto.request.ResidentOtpVerifyRequest;
import com.realestate.emi.dto.response.ApiResponse;
import com.realestate.emi.dto.response.ResidentLoginResponse;
import com.realestate.emi.exception.UnauthorizedException;
import com.realestate.emi.security.CustomPrincipal;
import com.realestate.emi.service.ResidentAuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/resident/auth")
@RequiredArgsConstructor
public class ResidentAuthController {

    private final ResidentAuthService residentAuthService;

    @PostMapping("/request-otp")
    public ResponseEntity<ApiResponse<Void>> requestOtp(@Valid @RequestBody ResidentOtpRequest request) {
        residentAuthService.requestOtp(request);
        return ResponseEntity.ok(ApiResponse.success(null, "OTP sent successfully"));
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<ApiResponse<ResidentLoginResponse>> verifyOtp(@Valid @RequestBody ResidentOtpVerifyRequest request) {
        ResidentLoginResponse response = residentAuthService.verifyOtp(request);
        return ResponseEntity.ok(ApiResponse.success(response, "Login successful"));
    }

    @PostMapping("/refresh")
    @PreAuthorize("hasRole('RESIDENT')")
    public ResponseEntity<ApiResponse<ResidentLoginResponse>> refresh(@AuthenticationPrincipal CustomPrincipal principal) {
        if (principal == null || !principal.isResident()) {
            throw new UnauthorizedException("Only resident sessions can be refreshed here");
        }
        ResidentLoginResponse response = residentAuthService.refresh(principal.getEmail());
        return ResponseEntity.ok(ApiResponse.success(response, "Token refreshed"));
    }

    @PostMapping("/logout")
    @PreAuthorize("hasRole('RESIDENT')")
    public ResponseEntity<ApiResponse<Void>> logout(Authentication auth) {
        if (auth != null && auth.getPrincipal() instanceof CustomPrincipal principal && principal.isResident()) {
            residentAuthService.logout(principal.getEmail());
        }
        return ResponseEntity.ok(ApiResponse.success(null, "Logged out successfully"));
    }
}
