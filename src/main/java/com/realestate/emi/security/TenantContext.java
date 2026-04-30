package com.realestate.emi.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class TenantContext {

    public Long getCurrentOrganizationId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof CustomPrincipal principal) {
            return principal.getOrganizationId();
        }
        return null;
    }

    public String getCurrentUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof CustomPrincipal principal) {
            return principal.getEmail();
        }
        if (auth != null) return auth.getName();
        return "unknown";
    }

    /**
     * Resident-only: returns the residentId claim from the current JWT.
     * Returns null when the principal is not a resident (admin/supervisor sessions).
     */
    public Long getCurrentResidentId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof CustomPrincipal principal) {
            return principal.getResidentId();
        }
        return null;
    }

    /**
     * Resident-only: returns the flatId claim from the current JWT.
     */
    public Long getCurrentFlatId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof CustomPrincipal principal) {
            return principal.getFlatId();
        }
        return null;
    }

    public Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof CustomPrincipal principal) {
            return principal.getUserId();
        }
        return null;
    }
}
