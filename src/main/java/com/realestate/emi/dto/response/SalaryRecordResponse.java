package com.realestate.emi.dto.response;

import com.realestate.emi.enums.SalaryStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SalaryRecordResponse {

    private Long id;
    private Long staffId;
    private String staffName;
    private String staffRole;
    private Integer year;
    private Integer month;
    private BigDecimal baseSalary;
    private Integer workingDays;
    private Integer presentDays;
    private Integer halfDays;
    private Integer absentDays;
    private BigDecimal overtimeHours;
    private BigDecimal overtimePay;
    private BigDecimal deductions;
    private BigDecimal bonus;
    private BigDecimal netSalary;
    private BigDecimal amountPaid;
    private BigDecimal balanceDue;
    private SalaryStatus status;
    private LocalDate paymentDate;
    private String remarks;
}
