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
public class MonthlyAnalyticsResponse {

    private int year;
    private int month;

    private long paymentsCompleted;
    private long paymentsPending;
    private long paymentsPartial;

    private BigDecimal amountReceived;
    private BigDecimal amountPending;
}
