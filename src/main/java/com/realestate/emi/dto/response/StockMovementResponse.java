package com.realestate.emi.dto.response;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockMovementResponse {

    private Long materialId;
    private String materialName;
    private String category;
    private String unit;
    private BigDecimal openingStock;
    private BigDecimal totalInward;
    private BigDecimal totalOutward;
    private BigDecimal totalWastage;
    private BigDecimal totalDamage;
    private BigDecimal totalAdjustments;
    private BigDecimal totalReturns;
    private BigDecimal closingStock;
    private BigDecimal netMovement;
}
