package com.realestate.emi.dto.response;

import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DealMaterialCostResponse {

    private Long dealId;
    private BigDecimal totalMaterialCost;
    private List<StockTransactionResponse> transactions;
}
