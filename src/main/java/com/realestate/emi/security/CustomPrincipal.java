package com.realestate.emi.security;

import io.jsonwebtoken.Claims;
import lombok.Getter;

import java.security.Principal;

@Getter
public class CustomPrincipal implements Principal {

    private final Long userId;
    private final String email;
    private final Long organizationId;
    private final Claims claims;

    public CustomPrincipal(Long userId, String email, Long organizationId, Claims claims) {
        this.userId = userId;
        this.email = email;
        this.organizationId = organizationId;
        this.claims = claims;
    }

    @Override
    public String getName() {
        return email;
    }
}
