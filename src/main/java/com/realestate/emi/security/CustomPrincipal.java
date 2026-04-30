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

    /**
     * Resident-context: returns residentId claim if the JWT was issued for a resident,
     * otherwise null. Safe additive helper for the society layer.
     */
    public Long getResidentId() {
        if (claims == null) return null;
        Object v = claims.get("residentId");
        return v instanceof Number n ? n.longValue() : null;
    }

    /**
     * Resident-context: returns flatId claim if the JWT was issued for a resident,
     * otherwise null.
     */
    public Long getFlatId() {
        if (claims == null) return null;
        Object v = claims.get("flatId");
        return v instanceof Number n ? n.longValue() : null;
    }

    /**
     * Returns true if this principal represents a resident-app session.
     */
    public boolean isResident() {
        if (claims == null) return false;
        Object t = claims.get("tokenType");
        return "RESIDENT".equals(t);
    }
}
