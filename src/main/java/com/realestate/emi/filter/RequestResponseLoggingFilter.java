package com.realestate.emi.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@Component
@Order(Integer.MIN_VALUE + 10)
public class RequestResponseLoggingFilter extends OncePerRequestFilter {

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/actuator")
                || path.startsWith("/health")
                || path.contains("favicon.ico");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        long startTime = System.currentTimeMillis();
        String method = request.getMethod();
        String path = request.getRequestURI();
        String actor = getActor();

        log.info("→ {} {} [{}]", method, path, actor);

        try {
            filterChain.doFilter(request, response);
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            int status = response.getStatus();
            log.info("← {} {} {} in {}ms [{}]", status, method, path, duration, actor);
        }
    }

    private String getActor() {
        String userId = MDC.get("userId");
        String userEmail = MDC.get("userEmail");
        if (userEmail != null) {
            return userEmail;
        }
        if (userId != null) {
            return "userId:" + userId;
        }
        return "anonymous";
    }
}
