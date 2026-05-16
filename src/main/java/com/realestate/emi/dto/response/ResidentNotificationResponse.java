package com.realestate.emi.dto.response;

import com.realestate.emi.enums.NotificationCategory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResidentNotificationResponse {

    private Long id;
    private String title;
    private String body;
    private NotificationCategory category;
    private String referenceType;
    private Long referenceId;
    private LocalDateTime readAt;
    private LocalDateTime createdAt;
    private boolean isRead;
}
