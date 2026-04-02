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
public class DashboardResponse {

    private Long totalDeals;
    private Long activeDeals;
    private Long completedDeals;
    private Long defaultedDeals;
    private BigDecimal totalDealAmount;
    private BigDecimal totalReceived;
    private BigDecimal totalOutstanding;
}
