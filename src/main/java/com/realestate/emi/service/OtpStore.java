package com.realestate.emi.service;

import com.realestate.emi.dto.request.AdminRegisterRequest;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class OtpStore {

    private static final long OTP_TTL_SECONDS = 300; // 5 minutes

    private record Entry(String otp, AdminRegisterRequest request, Instant expiresAt) {}

    private final Map<String, Entry> store = new ConcurrentHashMap<>();

    public void save(String mobile, String otp, AdminRegisterRequest request) {
        store.put(mobile, new Entry(otp, request, Instant.now().plusSeconds(OTP_TTL_SECONDS)));
    }

    public boolean verify(String mobile, String otp) {
        Entry entry = store.get(mobile);
        if (entry == null || Instant.now().isAfter(entry.expiresAt())) {
            store.remove(mobile);
            return false;
        }
        return entry.otp().equals(otp);
    }

    public AdminRegisterRequest getRequest(String mobile) {
        Entry entry = store.get(mobile);
        return entry != null ? entry.request() : null;
    }

    public void remove(String mobile) {
        store.remove(mobile);
    }
}
