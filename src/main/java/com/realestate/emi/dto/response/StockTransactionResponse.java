package com.realestate.emi.dto.response;

import com.realestate.emi.enums.TransactionType;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockTransactionResponse {

    private Long id;
    private Long materialId;
    private String materialName;
    private TransactionType type;
    private BigDecimal quantity;
    private BigDecimal unitCostAtTime;
    private BigDecimal totalCost;
    private Long dealId;
    private Long installmentPhaseId;
    private String referenceNumber;
    private String remarks;
    private LocalDateTime transactionDate;
    private String transactedBy;
}
