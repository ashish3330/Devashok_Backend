package com.realestate.emi.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Notification extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;  // null = for all admins in org

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "deal_id")
    private Deal deal;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "installment_phase_id")
    private InstallmentPhase installmentPhase;

    @Column(name = "type", nullable = false, length = 30)
    private String type;  // OVERDUE, DUE_TODAY, DUE_TOMORROW, DUE_SOON, INTEREST_APPLIED

    @Column(name = "severity", nullable = false, length = 15)
    private String severity;  // critical, warning, info

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "message", columnDefinition = "TEXT")
    private String message;

    @Column(name = "amount", precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(name = "deadline")
    private LocalDate deadline;

    @Column(name = "is_read", nullable = false)
    @Builder.Default
    private Boolean isRead = false;

    @Column(name = "read_at")
    private LocalDateTime readAt;

    @Column(name = "notification_key", nullable = false, length = 100)
    private String notificationKey;  // unique key like "DEAL-1-PHASE-2-2026-04-14" to prevent duplicates
}
