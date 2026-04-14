package com.realestate.emi.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class InstallmentPlanTemplateRequest {

    @NotBlank(message = "Phase name is required")
    private String phaseName;

    @NotNull(message = "Phase order is required")
    private Integer phaseOrder;

    @NotNull(message = "Percentage is required")
    @DecimalMin(value = "0.01", message = "Percentage must be greater than 0")
    @DecimalMax(value = "100.00", message = "Percentage must not exceed 100")
    private BigDecimal percentageOfTotal;

    private String description;
}
