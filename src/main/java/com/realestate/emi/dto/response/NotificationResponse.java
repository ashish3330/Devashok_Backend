package com.realestate.emi.dto.response;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class NotificationResponse {
    private int totalCount;
    private int urgentCount;
    private List<NotificationItem> items;

    @Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
    public static class NotificationItem {
        private Long id;
        private String type;       // "OVERDUE", "DUE_TODAY", "DUE_TOMORROW", "DUE_SOON"
        private String title;
        private String message;
        private Long dealId;
        private Long phaseId;
        private String customerName;
        private String phaseName;
        private BigDecimal amount;
        private LocalDate deadline;
        private Long daysOverdue;
        private String severity;   // "critical", "warning", "info"
        private Boolean isRead;
    }
}
