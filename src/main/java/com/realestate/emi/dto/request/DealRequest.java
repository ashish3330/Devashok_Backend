package com.realestate.emi.dto.request;

import com.realestate.emi.util.ValidEmiTenure;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
public class DealRequest {

    @NotNull(message = "Customer ID is required")
    private Long customerId;

    @NotNull(message = "Property type ID is required")
    private Long propertyTypeId;

    @NotBlank(message = "Property description is required")
    private String propertyDescription;

    @NotNull(message = "Total amount is required")
    @DecimalMin(value = "0.01", message = "Total amount must be greater than 0")
    private BigDecimal totalAmount;

    @NotNull(message = "Initial deposit is required")
    @DecimalMin(value = "0.00", message = "Initial deposit must be 0 or greater")
    private BigDecimal initialDeposit;

    @NotNull(message = "EMI tenure is required")
    @ValidEmiTenure
    private Integer emiTenureMonths;

    @DecimalMin(value = "0.00", message = "Interest rate must be 0 or greater")
    @DecimalMax(value = "36.00", message = "Interest rate must not exceed 36%")
    private BigDecimal interestRatePercent = BigDecimal.ZERO;

    @NotNull(message = "Deal date is required")
    private LocalDate dealDate;
}
