package com.realestate.emi.entity;

import com.realestate.emi.enums.PhaseStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "installment_phases")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InstallmentPhase extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "deal_id", nullable = false)
    private Deal deal;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "template_id")
    private InstallmentPlanTemplate template;

    @Column(name = "phase_name", nullable = false)
    private String phaseName;

    @Column(name = "phase_order", nullable = false)
    private Integer phaseOrder;

    @Column(name = "percentage", nullable = false, precision = 5, scale = 2)
    private BigDecimal percentage;

    @Column(name = "due_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal dueAmount;

    @Column(name = "paid_amount", precision = 19, scale = 2)
    @Builder.Default
    private BigDecimal paidAmount = BigDecimal.ZERO;

    @Column(name = "interest_amount", precision = 19, scale = 2)
    @Builder.Default
    private BigDecimal interestAmount = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private PhaseStatus status = PhaseStatus.PENDING;

    @Column(name = "milestone_description", columnDefinition = "TEXT")
    private String milestoneDescription;

    @Column(name = "marked_due_at")
    private LocalDateTime markedDueAt;

    @Column(name = "due_deadline")
    private LocalDate dueDeadline;

    @Column(name = "construction_completed_at")
    private LocalDateTime constructionCompletedAt;
}
