package com.realestate.emi.security;

import com.realestate.emi.entity.Resident;
import com.realestate.emi.entity.ResidentUser;
import com.realestate.emi.entity.User;
import com.realestate.emi.enums.Role;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class JwtService {

    private final String jwtSecret;
    private final long jwtExpiration;
    private final Key signingKey;

    public JwtService(@Value("${jwt.secret}") String jwtSecret,
                      @Value("${jwt.expiration}") long jwtExpiration) {
        this.jwtSecret = jwtSecret;
        this.jwtExpiration = jwtExpiration;
        this.signingKey = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }

    public String generateToken(User user) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpiration);

        return Jwts.builder()
                .setSubject(user.getUsername())
                .addClaims(Map.of(
                        "userId", user.getId(),
                        "email", user.getUsername(),
                        "roles", List.of(user.getRole().name()),
                        "organizationId", user.getOrganization() != null ? user.getOrganization().getId() : 0
                ))
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(signingKey, SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * Issue a JWT for a resident (society/resident-app login flow).
     * Carries: userId (resident-user id), residentId, flatId, organizationId, role=RESIDENT.
     * Subject is the resident's primary phone (no email for residents).
     */
    public String generateResidentToken(ResidentUser residentUser) {
        Resident resident = residentUser.getResident();
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpiration);

        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", residentUser.getId());
        claims.put("residentId", resident.getId());
        claims.put("flatId", resident.getFlat() != null ? resident.getFlat().getId() : null);
        claims.put("phone", residentUser.getPhone());
        claims.put("roles", List.of(Role.RESIDENT.name()));
        claims.put("organizationId", resident.getOrganization() != null ? resident.getOrganization().getId() : 0);
        claims.put("tokenType", "RESIDENT");

        return Jwts.builder()
                .setSubject(residentUser.getPhone())
                .addClaims(claims)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(signingKey, SignatureAlgorithm.HS256)
                .compact();
    }

    public long getJwtExpirationMillis() {
        return jwtExpiration;
    }
}
