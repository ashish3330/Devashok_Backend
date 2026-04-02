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
public class OverdueAnalysisResponse {

    private long totalOverdueCount;
    private BigDecimal totalOverdueAmount;

    private AgingBucket days1To30;
    private AgingBucket days31To60;
    private AgingBucket days61To90;
    private AgingBucket days90Plus;  // NPA zone
}
