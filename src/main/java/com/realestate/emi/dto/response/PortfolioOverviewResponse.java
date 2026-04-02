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
public class PortfolioOverviewResponse {

    private long totalDeals;
    private long activeDeals;
    private long completedDeals;
    private long defaultedDeals;

    private BigDecimal totalPortfolioValue;
    private BigDecimal totalCollected;
    private BigDecimal totalOutstanding;
    private double collectionEfficiencyPercent;

    private long npaDeals;           // deals with any EMI overdue 90+ days
    private double npaRatePercent;

    private BigDecimal averageDealValue;
    private BigDecimal averageEmiAmount;

    private long totalCustomers;
}
