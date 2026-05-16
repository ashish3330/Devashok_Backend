package com.realestate.emi.controller;

import com.realestate.emi.dto.response.ApiResponse;
import com.realestate.emi.dto.response.ResidentNotificationResponse;
import com.realestate.emi.service.ResidentNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/resident/notifications")
@RequiredArgsConstructor
@PreAuthorize("hasRole('RESIDENT')")
public class ResidentNotificationController {

    private final ResidentNotificationService residentNotificationService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<ResidentNotificationResponse>>> feed() {
        return ResponseEntity.ok(ApiResponse.success(
                residentNotificationService.feed(),
                "Notifications retrieved successfully"));
    }

    @GetMapping("/unread-count")
    public ResponseEntity<ApiResponse<Map<String, Long>>> unreadCount() {
        long count = residentNotificationService.unreadCount();
        return ResponseEntity.ok(ApiResponse.success(
                Map.of("count", count),
                "Unread count retrieved"));
    }

    @PostMapping("/{id}/read")
    public ResponseEntity<ApiResponse<ResidentNotificationResponse>> markRead(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(
                residentNotificationService.markRead(id),
                "Notification marked as read"));
    }

    @PostMapping("/read-all")
    public ResponseEntity<ApiResponse<Map<String, Integer>>> markAllRead() {
        int updated = residentNotificationService.markAllRead();
        return ResponseEntity.ok(ApiResponse.success(
                Map.of("updated", updated),
                "All notifications marked as read"));
    }
}
