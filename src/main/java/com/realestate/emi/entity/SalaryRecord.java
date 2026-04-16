package com.realestate.emi.entity;

import com.realestate.emi.enums.SalaryStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "salary_records", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"staff_id", "year", "month"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SalaryRecord extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "staff_id", nullable = false)
    private Staff staff;

    @Column(name = "year", nullable = false)
    private Integer year;

    @Column(name = "month", nullable = false)
    private Integer month;

    @Column(name = "base_salary", nullable = false, precision = 19, scale = 2)
    private BigDecimal baseSalary;

    @Column(name = "working_days", nullable = false)
    private Integer workingDays;

    @Column(name = "present_days", nullable = false)
    @Builder.Default
    private Integer presentDays = 0;

    @Column(name = "half_days", nullable = false)
    @Builder.Default
    private Integer halfDays = 0;

    @Column(name = "absent_days", nullable = false)
    @Builder.Default
    private Integer absentDays = 0;

    @Column(name = "overtime_hours", precision = 6, scale = 2)
    @Builder.Default
    private BigDecimal overtimeHours = BigDecimal.ZERO;

    @Column(name = "overtime_pay", precision = 19, scale = 2)
    @Builder.Default
    private BigDecimal overtimePay = BigDecimal.ZERO;

    @Column(name = "deductions", precision = 19, scale = 2)
    @Builder.Default
    private BigDecimal deductions = BigDecimal.ZERO;

    @Column(name = "bonus", precision = 19, scale = 2)
    @Builder.Default
    private BigDecimal bonus = BigDecimal.ZERO;

    // ── Salary structure breakdown ──────────────────────────────────────────

    @Column(name = "basic_pay", precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal basicPay = BigDecimal.ZERO;

    @Column(name = "hra", precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal hra = BigDecimal.ZERO;

    @Column(name = "conveyance_allowance", precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal conveyanceAllowance = BigDecimal.ZERO;

    @Column(name = "special_allowance", precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal specialAllowance = BigDecimal.ZERO;

    // ── Statutory deductions ────────────────────────────────────────────────

    @Column(name = "pf_deduction", precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal pfDeduction = BigDecimal.ZERO;

    @Column(name = "esi_deduction", precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal esiDeduction = BigDecimal.ZERO;

    @Column(name = "professional_tax", precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal professionalTax = BigDecimal.ZERO;

    // ── Advance deduction ───────────────────────────────────────────────────

    @Column(name = "advance_deduction", precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal advanceDeduction = BigDecimal.ZERO;

    // ── Gross and net ───────────────────────────────────────────────────────

    @Column(name = "gross_earnings", precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal grossEarnings = BigDecimal.ZERO;

    @Column(name = "total_deductions", precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal totalDeductions = BigDecimal.ZERO;

    @Column(name = "net_salary", nullable = false, precision = 19, scale = 2)
    private BigDecimal netSalary;

    @Column(name = "amount_paid", precision = 19, scale = 2)
    @Builder.Default
    private BigDecimal amountPaid = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private SalaryStatus status = SalaryStatus.PENDING;

    @Column(name = "payment_date")
    private LocalDate paymentDate;

    // ── Payment tracking ────────────────────────────────────────────────────

    @Column(name = "payment_method", length = 20)
    private String paymentMethod;

    @Column(name = "payment_reference")
    private String paymentReference;

    @Column(name = "holiday_count")
    @Builder.Default
    private Integer holidayCount = 0;

    @Column(name = "remarks", columnDefinition = "TEXT")
    private String remarks;
}
