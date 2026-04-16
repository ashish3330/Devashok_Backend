package com.realestate.emi.dto.request;

import com.realestate.emi.enums.TransactionType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class StockTransactionRequest {

    @NotNull(message = "Material ID is required")
    private Long materialId;

    @NotNull(message = "Transaction type is required")
    private TransactionType type;

    @NotNull(message = "Quantity is required")
    @DecimalMin(value = "0.01", message = "Quantity must be greater than 0")
    private BigDecimal quantity;

    private BigDecimal unitCost;

    private Long dealId;

    private Long installmentPhaseId;

    private Long supplierId;

    private String referenceNumber;

    private String remarks;
}
