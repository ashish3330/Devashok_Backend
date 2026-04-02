package com.realestate.emi.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OverdueEmiItem {

    private Long emiId;
    private Long dealId;
    private String customerName;
    private String customerPhone;
    private String propertyType;
    private LocalDate dueDate;
    private BigDecimal dueAmount;
    private BigDecimal paidAmount;
    private BigDecimal balanceDue;
    private String status;
    private long daysOverdue;
}
