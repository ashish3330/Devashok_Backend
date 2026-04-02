package com.realestate.emi.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DealDistributionResponse {

    private List<PropertyTypeBreakdown> byPropertyType;
    private List<StatusBreakdown> byStatus;

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PropertyTypeBreakdown {
        private String propertyType;
        private long dealCount;
        private BigDecimal totalValue;
        private double percentOfPortfolio;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StatusBreakdown {
        private String status;
        private long count;
        private double percentOfTotal;
    }
}
