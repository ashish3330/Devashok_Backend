package com.realestate.emi.dto.response;

import com.realestate.emi.enums.MaintenanceBillStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MaintenanceBillResponse {

    private Long id;
    private Long flatId;
    private String flatNumber;
    private String blockName;
    private String billMonth;
    private BigDecimal baseAmount;
    private BigDecimal sinkingFund;
    private BigDecimal otherCharges;
    private BigDecimal lateFee;
    private BigDecimal totalAmount;
    private BigDecimal paidAmount;
    private BigDecimal outstandingAmount;
    private LocalDate dueDate;
    private Long daysOverdue;
    private MaintenanceBillStatus status;
    private LocalDateTime generatedAt;
    private LocalDateTime createdAt;
    private List<MaintenancePaymentResponse> payments;
}
