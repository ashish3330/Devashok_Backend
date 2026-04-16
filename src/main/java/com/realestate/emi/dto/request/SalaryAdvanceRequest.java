package com.realestate.emi.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
public class SalaryAdvanceRequest {

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "1.00", message = "Amount must be at least 1")
    private BigDecimal amount;

    @DecimalMin(value = "0.00", message = "Monthly deduction must be 0 or greater")
    private BigDecimal monthlyDeductionAmount;

    private LocalDate advanceDate;

    private String remarks;
}
