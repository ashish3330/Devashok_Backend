package com.realestate.emi.dto.response;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InstallmentPlanTemplateResponse {

    private Long id;
    private String phaseName;
    private Integer phaseOrder;
    private BigDecimal percentageOfTotal;
    private String description;
    private Boolean isActive;
}
