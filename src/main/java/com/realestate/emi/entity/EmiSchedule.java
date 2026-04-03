package com.realestate.emi.entity;

import com.realestate.emi.enums.EmiStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "emi_schedules")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmiSchedule extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "deal_id", nullable = false)
    private Deal deal;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Column(name = "due_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal dueAmount;

    @Column(name = "paid_amount", precision = 19, scale = 2)
    @Builder.Default
    private BigDecimal paidAmount = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private EmiStatus status = EmiStatus.PENDING;

    @Column(name = "bounced", nullable = false)
    @Builder.Default
    private boolean bounced = false;

    @Column(name = "bounce_charges", precision = 19, scale = 2)
    @Builder.Default
    private BigDecimal bounceCharges = BigDecimal.ZERO;
}
