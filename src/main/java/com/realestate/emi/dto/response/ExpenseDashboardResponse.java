package com.realestate.emi.dto.response;

import lombok.*;
import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExpenseDashboardResponse {

    // ── Summary ─────────────────────────────────────────────────────────────────
    private BigDecimal totalExpenses;
    private BigDecimal salaryExpenses;
    private BigDecimal materialExpenses;
    private BigDecimal supplierPayments;
    private BigDecimal previousMonthTotal;
    private double monthOverMonthChange; // percentage

    // ── Salary breakdown ────────────────────────────────────────────────────────
    private int staffCount;
    private BigDecimal grossPayroll;
    private BigDecimal netPayroll;
    private BigDecimal totalSalaryPaid;
    private BigDecimal pendingSalary;
    private BigDecimal averageSalary;
    private BigDecimal totalOvertimePay;
    private BigDecimal totalAdvancesGiven;

    // ── Material breakdown ──────────────────────────────────────────────────────
    private BigDecimal totalPurchases;
    private BigDecimal totalWastage;
    private BigDecimal totalDamage;
    private BigDecimal outstandingSupplierBalance;
    private long lowStockAlerts;
    private long outOfStockCount;
    private List<CategoryExpense> categoryBreakdown;

    // ── Trends (last 6 months) ──────────────────────────────────────────────────
    private List<MonthlyExpenseTrend> trends;

    // ── Top items ───────────────────────────────────────────────────────────────
    private List<TopExpenseItem> topStaffBySalary;
    private List<TopExpenseItem> topMaterialsBySpend;
    private List<TopExpenseItem> topSuppliersByPayment;

    // ── Legacy fields (backward compatibility) ──────────────────────────────────
    private BigDecimal totalMonthlySalaryBudget;
    private BigDecimal currentMonthSalaryPaid;
    private BigDecimal currentMonthSalaryPending;
    private Long totalActiveStaff;
    private Long pendingSalaryCount;
    private BigDecimal totalInventoryValue;
    private BigDecimal totalMaterialExpenseThisMonth;
    private Long totalMaterials;
    private BigDecimal totalExpensesThisMonth;
    private List<MonthlyExpenseItem> expenseTrend;

    // ── Inner classes ───────────────────────────────────────────────────────────

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CategoryExpense {
        private String category;
        private BigDecimal amount;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MonthlyExpenseTrend {
        private int year;
        private int month;
        private String monthLabel;
        private BigDecimal salaryExpense;
        private BigDecimal materialExpense;
        private BigDecimal supplierPayment;
        private BigDecimal totalExpense;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TopExpenseItem {
        private String name;
        private BigDecimal amount;
        private String subtitle;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MonthlyExpenseItem {
        private int year;
        private int month;
        private String monthLabel;
        private BigDecimal salaryExpense;
        private BigDecimal materialExpense;
        private BigDecimal totalExpense;
    }
}
