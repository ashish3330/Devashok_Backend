package com.realestate.emi.dto.response;

import com.realestate.emi.enums.PhaseStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InstallmentPhaseResponse {

    private Long id;
    private Long dealId;
    private String phaseName;
    private Integer phaseOrder;
    private BigDecimal percentage;
    private BigDecimal dueAmount;
    private BigDecimal paidAmount;
    private BigDecimal interestAmount;
    private BigDecimal totalDue;
    private BigDecimal balanceDue;
    private PhaseStatus status;
    private String milestoneDescription;
    private LocalDateTime markedDueAt;
    private LocalDate dueDeadline;
    private Long daysOverdue;
    private LocalDateTime constructionCompletedAt;
}
