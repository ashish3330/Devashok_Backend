package com.realestate.emi.service;

import com.realestate.emi.dto.request.AdminRegisterRequest;
import com.realestate.emi.entity.User;
import com.realestate.emi.enums.Role;
import com.realestate.emi.exception.ServiceException;
import com.realestate.emi.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminRegisterService {

    private final OtpStore otpStore;
    private final SmsService smsService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    private static final SecureRandom RANDOM = new SecureRandom();

    public void sendOtp(AdminRegisterRequest request) {
        if (userRepository.findByUsername(request.getUsername()).isPresent()) {
            throw new ServiceException("Username already exists");
        }
        if (userRepository.findByMobile(request.getMobile()).isPresent()) {
            throw new ServiceException("Mobile number already registered");
        }

        String otp = String.format("%06d", RANDOM.nextInt(1_000_000));
        otpStore.save(request.getMobile(), otp, request);
        smsService.sendOtp(request.getMobile(), otp);
        log.info("OTP registration initiated for mobile: {}", request.getMobile());
    }

    @Transactional
    public void verifyOtpAndCreateAdmin(String mobile, String otp) {
        if (!otpStore.verify(mobile, otp)) {
            throw new ServiceException("Invalid or expired OTP");
        }

        AdminRegisterRequest req = otpStore.getRequest(mobile);
        if (req == null) {
            throw new ServiceException("Registration session expired. Please start again.");
        }

        User admin = User.builder()
                .username(req.getUsername())
                .password(passwordEncoder.encode(req.getPassword()))
                .fullName(req.getFullName())
                .mobile(req.getMobile())
                .role(req.getRole() != null ? req.getRole() : Role.ADMIN)
                .build();

        userRepository.save(admin);
        otpStore.remove(mobile);
        log.info("Admin created successfully: {}", req.getUsername());
    }
}
