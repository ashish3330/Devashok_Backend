package com.realestate.emi.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DailyHelpAssignmentRequest {

    @NotNull
    private Long helpId;

    private LocalDate startDate;

    @DecimalMin(value = "0.0", inclusive = true)
    private BigDecimal monthlySalary;
}
