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
public class MonthlyTrendItem {

    private int year;
    private int month;
    private String monthLabel;   // e.g. "Apr 2026"

    private long emisPaid;
    private long emisPending;
    private long emisPartial;

    private BigDecimal amountCollected;
    private BigDecimal amountPending;
    private double collectionRatePercent;
}
