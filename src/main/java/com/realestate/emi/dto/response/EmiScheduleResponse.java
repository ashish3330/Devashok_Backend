package com.realestate.emi.dto.response;

import com.realestate.emi.enums.EmiStatus;
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
public class EmiScheduleResponse {

    private Long id;
    private LocalDate dueDate;
    private BigDecimal baseEmiAmount;
    private BigDecimal bounceCharges;
    private BigDecimal totalDueAmount;
    private BigDecimal paidAmount;
    private EmiStatus status;
    private boolean bounced;
    private BigDecimal remaining;
}
