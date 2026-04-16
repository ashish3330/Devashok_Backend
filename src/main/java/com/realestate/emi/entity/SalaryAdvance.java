package com.realestate.emi.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "salary_advances")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SalaryAdvance extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "staff_id", nullable = false)
    private Staff staff;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(name = "balance_remaining", nullable = false, precision = 12, scale = 2)
    private BigDecimal balanceRemaining;

    @Column(name = "advance_date")
    private LocalDate advanceDate;

    @Column(name = "monthly_deduction_amount", precision = 12, scale = 2)
    private BigDecimal monthlyDeductionAmount;

    @Column(name = "status", length = 20)
    private String status;

    @Column(name = "remarks")
    private String remarks;

    @Column(name = "approved_by")
    private String approvedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;
}
