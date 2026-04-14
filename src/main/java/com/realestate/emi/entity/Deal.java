package com.realestate.emi.entity;

import com.realestate.emi.enums.DealStatus;
import com.realestate.emi.enums.PlanType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "deals")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Deal extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id")
    private Organization organization;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "property_type_id", nullable = false)
    private PropertyType propertyType;

    @Column(name = "property_description", nullable = false, columnDefinition = "TEXT")
    private String propertyDescription;

    @Column(name = "total_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "initial_deposit", nullable = false, precision = 19, scale = 2)
    private BigDecimal initialDeposit;

    @Column(name = "emi_tenure_months")
    private Integer emiTenureMonths;

    @Column(name = "interest_rate_percent", precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal interestRatePercent = BigDecimal.ZERO;

    @Column(name = "deal_date", nullable = false)
    private LocalDate dealDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private DealStatus status = DealStatus.ACTIVE;

    @Column(name = "emi_amount_per_month", precision = 19, scale = 2)
    private BigDecimal emiAmountPerMonth;

    @Column(name = "total_payable_after_deposit", precision = 19, scale = 2)
    private BigDecimal totalPayableAfterDeposit;

    @Enumerated(EnumType.STRING)
    @Column(name = "plan_type", nullable = false, length = 30)
    @Builder.Default
    private PlanType planType = PlanType.EMI;

    @Column(name = "next_due_date")
    private LocalDate nextDueDate;

    @OneToMany(mappedBy = "deal", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    @Builder.Default
    private List<EmiSchedule> emiSchedules = new ArrayList<>();

    @OneToMany(mappedBy = "deal", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    @Builder.Default
    private List<InstallmentPhase> installmentPhases = new ArrayList<>();

    @OneToMany(mappedBy = "deal", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    @Builder.Default
    private List<Payment> payments = new ArrayList<>();
}
