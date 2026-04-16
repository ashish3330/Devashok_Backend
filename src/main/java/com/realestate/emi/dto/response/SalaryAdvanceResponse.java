package com.realestate.emi.dto.response;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SalaryAdvanceResponse {

    private Long id;
    private Long staffId;
    private String staffName;
    private BigDecimal amount;
    private BigDecimal balanceRemaining;
    private BigDecimal monthlyDeductionAmount;
    private LocalDate advanceDate;
    private String status;
    private String remarks;
    private String approvedBy;
    private LocalDateTime createdAt;
}
