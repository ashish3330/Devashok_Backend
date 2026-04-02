package com.realestate.emi.dto.response;

import com.realestate.emi.enums.DealStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class DealSummaryResponse {

    private Long id;
    private Long customerId;
    private String customerName;
    private Long propertyTypeId;
    private String propertyTypeName;
    private String propertyDescription;
    private BigDecimal totalAmount;
    private BigDecimal initialDeposit;
    private Integer emiTenureMonths;
    private BigDecimal interestRatePercent;
    private BigDecimal emiAmountPerMonth;
    private BigDecimal totalPayableAfterDeposit;
    private LocalDate dealDate;
    private DealStatus status;
    private LocalDate nextDueDate;
    private BigDecimal totalPaid;
    private BigDecimal outstanding;
}
