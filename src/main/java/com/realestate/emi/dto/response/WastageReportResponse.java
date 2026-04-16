package com.realestate.emi.dto.response;

import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WastageReportResponse {

    private BigDecimal totalWastageValue;
    private BigDecimal totalDamageValue;
    private BigDecimal totalReturnValue;
    private List<MaterialWastageItem> breakdown;

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MaterialWastageItem {
        private Long materialId;
        private String materialName;
        private String category;
        private String unit;
        private BigDecimal wastageQuantity;
        private BigDecimal wastageValue;
        private BigDecimal damageQuantity;
        private BigDecimal damageValue;
        private BigDecimal returnQuantity;
        private BigDecimal returnValue;
    }
}
