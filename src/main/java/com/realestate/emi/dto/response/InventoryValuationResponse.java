package com.realestate.emi.dto.response;

import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryValuationResponse {

    private BigDecimal totalInventoryValue;
    private Long totalMaterials;
    private Long lowStockCount;
    private Long outOfStockCount;
    private List<MaterialResponse> materials;
}
