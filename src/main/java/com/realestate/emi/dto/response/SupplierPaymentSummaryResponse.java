package com.realestate.emi.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SupplierPaymentSummaryResponse {

    private BigDecimal totalInwardCost;
    private BigDecimal totalPaid;
    private BigDecimal outstandingBalance;
}
