package com.realestate.emi.security;

import io.jsonwebtoken.Claims;
import lombok.Getter;

import java.security.Principal;

@Getter
public class CustomPrincipal implements Principal {

    private final Long userId;
    private final String email;
    private final Claims claims;

    public CustomPrincipal(Long userId, String email, Claims claims) {
        this.userId = userId;
        this.email = email;
        this.claims = claims;
    }

    @Override
    public String getName() {
        return email;
    }
}
