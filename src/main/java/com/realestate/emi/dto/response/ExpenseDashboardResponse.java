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

    // Salary summary
    private BigDecimal totalMonthlySalaryBudget;
    private BigDecimal currentMonthSalaryPaid;
    private BigDecimal currentMonthSalaryPending;
    private Long totalActiveStaff;
    private Long pendingSalaryCount;

    // Material/Inventory summary
    private BigDecimal totalInventoryValue;
    private BigDecimal totalMaterialExpenseThisMonth;
    private Long totalMaterials;
    private Long lowStockAlerts;
    private Long outOfStockCount;

    // Combined
    private BigDecimal totalExpensesThisMonth;

    // Monthly expense trend (last 6 months)
    private List<MonthlyExpenseItem> expenseTrend;

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
