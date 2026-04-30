package com.realestate.emi.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MaintenanceBillGenerateRequest {

    @NotNull
    @Pattern(regexp = "^\\d{4}-\\d{2}$", message = "billMonth must be YYYY-MM")
    private String billMonth;

    @NotNull
    private LocalDate dueDate;

    @NotNull
    @DecimalMin(value = "0.0", inclusive = true)
    private BigDecimal baseAmount;

    @DecimalMin(value = "0.0", inclusive = true)
    private BigDecimal sinkingFund;

    @DecimalMin(value = "0.0", inclusive = true)
    private BigDecimal otherCharges;

    private Long blockId;

    @NotEmpty
    private List<Long> flatIds;
}
