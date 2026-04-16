package com.realestate.emi.dto.response;

import lombok.*;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StaffAnalyticsResponse {

    private int totalStaff;
    private int activeStaff;
    private int inactiveStaff;
    private Map<String, Integer> staffByDepartment;
    private Map<String, Integer> staffByEmploymentType;
    private BigDecimal totalMonthlyPayroll;
    private BigDecimal averageAttendanceRate;
    private int totalAdvancesOutstanding;
    private BigDecimal totalAdvanceAmount;
    private List<DepartmentSummary> departmentSummary;

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DepartmentSummary {
        private String department;
        private int count;
        private BigDecimal totalSalary;
        private BigDecimal averageSalary;
    }
}
