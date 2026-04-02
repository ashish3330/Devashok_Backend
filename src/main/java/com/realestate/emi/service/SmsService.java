package com.realestate.emi.service;

import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class SmsService {

    private static final String PLACEHOLDER_SID = "your_account_sid";

    @Value("${twilio.account-sid}")
    private String accountSid;

    @Value("${twilio.auth-token}")
    private String authToken;

    @Value("${twilio.from-number}")
    private String fromNumber;

    @PostConstruct
    public void init() {
        if (!isConfigured()) {
            log.warn("Twilio is NOT configured. OTPs will be logged to console (dev mode).");
            return;
        }
        Twilio.init(accountSid, authToken);
        log.info("Twilio SMS service initialized.");
    }

    public void sendOtp(String toMobile, String otp) {
        System.out.println("==================================================" + otp);
        if (!isConfigured()) {
            // Dev fallback: print OTP to logs so you can test without Twilio
            log.warn("==================================================");
            log.warn("  [DEV MODE] OTP for {} : {}", toMobile, otp);
            log.warn("==================================================");
            return;
        }
        try {
            Message.creator(
                    new PhoneNumber(toMobile),
                    new PhoneNumber(fromNumber),
                    "Your RealEstate EMI Tracker OTP is: " + otp + ". Valid for 5 minutes. Do not share."
            ).create();
            log.info("OTP SMS sent to {}", toMobile);
        } catch (Exception e) {
            log.error("Failed to send OTP to {}: {}", toMobile, e.getMessage());
            throw new RuntimeException("Failed to send OTP. Please try again.");
        }
    }

    private boolean isConfigured() {
        return accountSid != null && !accountSid.isBlank() && !accountSid.equals(PLACEHOLDER_SID);
    }
}
