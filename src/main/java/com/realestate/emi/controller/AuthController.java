package com.realestate.emi.controller;

import com.realestate.emi.dto.request.AdminRegisterRequest;
import com.realestate.emi.dto.request.LoginRequest;
import com.realestate.emi.dto.request.OtpVerifyRequest;
import com.realestate.emi.dto.response.ApiResponse;
import com.realestate.emi.dto.response.LoginResponse;
import com.realestate.emi.entity.User;
import com.realestate.emi.exception.UnauthorizedException;
import com.realestate.emi.repository.UserRepository;
import com.realestate.emi.security.JwtService;
import com.realestate.emi.service.AdminRegisterService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final AdminRegisterService adminRegisterService;

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request) {
        log.debug("Login attempt for username: {}", request.getUsername());
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword()));

            User user = userRepository.findByUsername(request.getUsername())
                    .orElseThrow(() -> new UnauthorizedException("Invalid username or password"));

            String token = jwtService.generateToken(user);

            LoginResponse response = LoginResponse.builder()
                    .token(token)
                    .tokenType("Bearer")
                    .username(user.getUsername())
                    .fullName(user.getFullName())
                    .build();

            log.info("Successful login for user: {}", request.getUsername());
            return ResponseEntity.ok(ApiResponse.success(response, "Login successful"));

        } catch (BadCredentialsException e) {
            log.warn("Failed login attempt for username: {}", request.getUsername());
            throw new UnauthorizedException("Invalid username or password");
        }
    }

    @PostMapping("/admin/send-otp")
    public ResponseEntity<ApiResponse<Void>> sendAdminOtp(@Valid @RequestBody AdminRegisterRequest request) {
        adminRegisterService.sendOtp(request);
        return ResponseEntity.ok(ApiResponse.success(null, "OTP sent to " + request.getMobile()));
    }

    @PostMapping("/admin/verify-otp")
    public ResponseEntity<ApiResponse<Void>> verifyAdminOtp(@Valid @RequestBody OtpVerifyRequest request) {
        adminRegisterService.verifyOtpAndCreateAdmin(request.getMobile(), request.getOtp());
        return ResponseEntity.ok(ApiResponse.success(null, "Admin registered successfully"));
    }
}
