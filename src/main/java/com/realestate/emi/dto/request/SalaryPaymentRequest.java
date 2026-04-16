package com.realestate.emi.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class SalaryPaymentRequest {

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    private BigDecimal amount;

    private BigDecimal bonus = BigDecimal.ZERO;

    private BigDecimal deductions = BigDecimal.ZERO;

    private String remarks;

    private String paymentMethod; // CASH, BANK_TRANSFER, UPI, CHEQUE

    private String paymentReference; // UTR/cheque number
}
