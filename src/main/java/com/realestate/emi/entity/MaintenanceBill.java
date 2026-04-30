package com.realestate.emi.entity;

import com.realestate.emi.enums.MaintenanceBillStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "society_maintenance_bills", uniqueConstraints = {
        @UniqueConstraint(name = "uk_maint_bill_org_flat_month", columnNames = {"organization_id", "flat_id", "bill_month"})
}, indexes = {
        @Index(name = "idx_maint_bill_status", columnList = "status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MaintenanceBill extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "flat_id", nullable = false)
    private Flat flat;

    @Column(name = "bill_month", nullable = false, length = 7)
    private String billMonth;

    @Column(name = "base_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal baseAmount;

    @Column(name = "sinking_fund", precision = 19, scale = 2)
    private BigDecimal sinkingFund;

    @Column(name = "other_charges", precision = 19, scale = 2)
    private BigDecimal otherCharges;

    @Column(name = "late_fee", precision = 19, scale = 2)
    private BigDecimal lateFee;

    @Column(name = "total_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "paid_amount", nullable = false, precision = 19, scale = 2)
    @Builder.Default
    private BigDecimal paidAmount = BigDecimal.ZERO;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private MaintenanceBillStatus status;

    @Column(name = "generated_at", nullable = false)
    private LocalDateTime generatedAt;
}
