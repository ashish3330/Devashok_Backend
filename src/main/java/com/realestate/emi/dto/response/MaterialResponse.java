package com.realestate.emi.dto.response;

import com.realestate.emi.enums.MaterialCategory;
import com.realestate.emi.enums.UnitType;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MaterialResponse {

    private Long id;
    private String name;
    private MaterialCategory category;
    private UnitType unit;
    private BigDecimal unitCost;
    private java.util.List<SupplierSummary> suppliers;

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SupplierSummary {
        private Long id;
        private String name;
    }
    private String hsnCode;
    private BigDecimal reorderLevel;
    private BigDecimal minimumThreshold;
    private BigDecimal currentQuantity;
    private BigDecimal stockValue;
    private Boolean isActive;
    private Boolean lowStock;
}
