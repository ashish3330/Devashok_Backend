package com.realestate.emi.service;

import com.realestate.emi.dto.request.ResidentOtpRequest;
import com.realestate.emi.dto.request.ResidentOtpVerifyRequest;
import com.realestate.emi.dto.response.ResidentLoginResponse;
import com.realestate.emi.entity.Flat;
import com.realestate.emi.entity.Organization;
import com.realestate.emi.entity.Resident;
import com.realestate.emi.entity.ResidentOtp;
import com.realestate.emi.entity.ResidentPhone;
import com.realestate.emi.entity.ResidentUser;
import com.realestate.emi.exception.ServiceException;
import com.realestate.emi.exception.UnauthorizedException;
import com.realestate.emi.repository.ResidentOtpRepository;
import com.realestate.emi.repository.ResidentPhoneRepository;
import com.realestate.emi.repository.ResidentRepository;
import com.realestate.emi.repository.ResidentUserRepository;
import com.realestate.emi.security.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ResidentAuthService {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final long OTP_TTL_MINUTES = 5;
    private static final int MAX_OTPS_PER_HOUR = 3;
    private static final int MAX_VERIFY_ATTEMPTS = 5;
    private static final String GENERIC_NOT_REGISTERED = "This phone is not registered. Please contact your society admin.";

    private final ResidentPhoneRepository residentPhoneRepository;
    private final ResidentOtpRepository residentOtpRepository;
    private final ResidentRepository residentRepository;
    private final ResidentUserRepository residentUserRepository;
    private final SmsService smsService;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public void requestOtp(ResidentOtpRequest request) {
        String phone = normalize(request.getPhone());

        // Look up phone whitelist
        Optional<ResidentPhone> phoneOpt = residentPhoneRepository.findByPhone(phone);
        if (phoneOpt.isEmpty()) {
            log.warn("OTP requested for unregistered phone: {}", maskPhone(phone));
            throw new UnauthorizedException(GENERIC_NOT_REGISTERED);
        }
        ResidentPhone residentPhone = phoneOpt.get();
        Resident resident = residentPhone.getResident();
        if (resident == null || Boolean.FALSE.equals(resident.getIsActive())) {
            log.warn("OTP requested for inactive resident phone: {}", maskPhone(phone));
            throw new UnauthorizedException(GENERIC_NOT_REGISTERED);
        }

        // Rate limit: max 3 OTPs per phone per hour
        long recent = residentOtpRepository.countByPhoneAndCreatedAtAfter(phone, LocalDateTime.now().minusHours(1));
        if (recent >= MAX_OTPS_PER_HOUR) {
            throw new ServiceException("Too many OTP requests. Please try again later.", "OTP_RATE_LIMITED");
        }

        String otp = String.format("%06d", RANDOM.nextInt(1_000_000));
        String otpHash = passwordEncoder.encode(otp);

        ResidentOtp record = ResidentOtp.builder()
                .phone(phone)
                .otpHash(otpHash)
                .expiresAt(LocalDateTime.now().plusMinutes(OTP_TTL_MINUTES))
                .attempts(0)
                .consumed(false)
                .build();
        residentOtpRepository.save(record);

        smsService.sendOtp(phone, otp);
        log.info("Resident OTP sent to {}", maskPhone(phone));
    }

    @Transactional
    public ResidentLoginResponse verifyOtp(ResidentOtpVerifyRequest request) {
        String phone = normalize(request.getPhone());

        ResidentOtp otpRecord = residentOtpRepository
                .findTopByPhoneAndConsumedFalseOrderByExpiresAtDesc(phone)
                .orElseThrow(() -> new UnauthorizedException("Invalid or expired OTP"));

        if (otpRecord.getAttempts() != null && otpRecord.getAttempts() >= MAX_VERIFY_ATTEMPTS) {
            otpRecord.setConsumed(true);
            residentOtpRepository.save(otpRecord);
            throw new UnauthorizedException("Too many attempts. Please request a new OTP.");
        }

        if (LocalDateTime.now().isAfter(otpRecord.getExpiresAt())) {
            otpRecord.setConsumed(true);
            residentOtpRepository.save(otpRecord);
            throw new UnauthorizedException("Invalid or expired OTP");
        }

        otpRecord.setAttempts((otpRecord.getAttempts() == null ? 0 : otpRecord.getAttempts()) + 1);
        if (!passwordEncoder.matches(request.getOtp(), otpRecord.getOtpHash())) {
            residentOtpRepository.save(otpRecord);
            throw new UnauthorizedException("Invalid or expired OTP");
        }

        otpRecord.setConsumed(true);
        residentOtpRepository.save(otpRecord);

        ResidentPhone residentPhone = residentPhoneRepository.findByPhone(phone)
                .orElseThrow(() -> new UnauthorizedException("Phone is no longer registered"));

        Resident resident = residentPhone.getResident();
        if (resident == null || Boolean.FALSE.equals(resident.getIsActive())) {
            throw new UnauthorizedException("Resident account is inactive");
        }

        residentPhone.setIsVerified(true);
        residentPhoneRepository.save(residentPhone);

        ResidentUser residentUser = residentUserRepository.findByResidentId(resident.getId())
                .orElseGet(() -> ResidentUser.builder()
                        .resident(resident)
                        .phone(phone)
                        .build());
        residentUser.setLastLoginAt(LocalDateTime.now());
        if (request.getFcmToken() != null && !request.getFcmToken().isBlank()) {
            residentUser.setFcmToken(request.getFcmToken());
        }
        if (request.getDeviceInfo() != null && !request.getDeviceInfo().isBlank()) {
            residentUser.setDeviceInfo(request.getDeviceInfo());
        }
        // Always keep phone in sync with the verified phone (in case admin changed primary)
        residentUser.setPhone(phone);
        ResidentUser savedUser = residentUserRepository.save(residentUser);

        String token = jwtService.generateResidentToken(savedUser);
        log.info("Resident login successful: residentId={}", resident.getId());
        return buildLoginResponse(token, savedUser, resident);
    }

    @Transactional
    public ResidentLoginResponse refresh(String currentResidentPhone) {
        // Lightweight refresh: re-issue token for the currently authenticated resident's phone.
        ResidentUser residentUser = residentUserRepository.findByPhone(currentResidentPhone)
                .orElseThrow(() -> new UnauthorizedException("Session not found"));
        Resident resident = residentUser.getResident();
        if (resident == null || Boolean.FALSE.equals(resident.getIsActive())) {
            throw new UnauthorizedException("Resident account is inactive");
        }
        residentUser.setLastLoginAt(LocalDateTime.now());
        residentUserRepository.save(residentUser);
        String token = jwtService.generateResidentToken(residentUser);
        return buildLoginResponse(token, residentUser, resident);
    }

    @Transactional
    public void logout(String currentResidentPhone) {
        residentUserRepository.findByPhone(currentResidentPhone).ifPresent(u -> {
            u.setFcmToken(null);
            residentUserRepository.save(u);
        });
    }

    private ResidentLoginResponse buildLoginResponse(String token, ResidentUser residentUser, Resident resident) {
        Flat flat = resident.getFlat();
        Organization org = resident.getOrganization();

        ResidentLoginResponse.FlatSummary flatSummary = flat == null ? null
                : ResidentLoginResponse.FlatSummary.builder()
                        .id(flat.getId())
                        .flatNumber(flat.getFlatNumber())
                        .floor(flat.getFloor())
                        .blockId(flat.getBlock() != null ? flat.getBlock().getId() : null)
                        .blockName(flat.getBlock() != null ? flat.getBlock().getName() : null)
                        .blockCode(flat.getBlock() != null ? flat.getBlock().getCode() : null)
                        .build();

        ResidentLoginResponse.OrganizationSummary orgSummary = org == null ? null
                : ResidentLoginResponse.OrganizationSummary.builder()
                        .id(org.getId())
                        .name(org.getName())
                        .code(org.getCode())
                        .build();

        ResidentLoginResponse.ResidentSummary residentSummary = ResidentLoginResponse.ResidentSummary.builder()
                .id(resident.getId())
                .fullName(resident.getFullName())
                .primaryPhone(resident.getPrimaryPhone())
                .email(resident.getEmail())
                .flat(flatSummary)
                .organization(orgSummary)
                .build();

        return ResidentLoginResponse.builder()
                .accessToken(token)
                .tokenType("Bearer")
                .expiresInMillis(jwtService.getJwtExpirationMillis())
                .resident(residentSummary)
                .build();
    }

    private String normalize(String phone) {
        return phone == null ? null : phone.trim();
    }

    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 4) return "****";
        return "****" + phone.substring(phone.length() - 4);
    }
}
