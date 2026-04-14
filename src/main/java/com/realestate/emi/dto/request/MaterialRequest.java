package com.realestate.emi.dto.request;

import com.realestate.emi.enums.MaterialCategory;
import com.realestate.emi.enums.UnitType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class MaterialRequest {

    @NotBlank(message = "Material name is required")
    private String name;

    @NotNull(message = "Category is required")
    private MaterialCategory category;

    @NotNull(message = "Unit type is required")
    private UnitType unit;

    @NotNull(message = "Unit cost is required")
    @DecimalMin(value = "0.01", message = "Unit cost must be greater than 0")
    private BigDecimal unitCost;

    private String hsnCode;

    @DecimalMin(value = "0", message = "Reorder level must be 0 or greater")
    private BigDecimal reorderLevel = BigDecimal.ZERO;

    @DecimalMin(value = "0", message = "Minimum threshold must be 0 or greater")
    private BigDecimal minimumThreshold = BigDecimal.ZERO;
}
