package com.realestate.emi.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class PurchaseOrderReceiveRequest {

    @NotEmpty(message = "At least one item must be received")
    @Valid
    private List<ReceiveItem> items;

    @Getter
    @Setter
    public static class ReceiveItem {
        @NotNull(message = "Item ID is required")
        private Long itemId;

        @NotNull(message = "Received quantity is required")
        @DecimalMin(value = "0.01", message = "Received quantity must be greater than 0")
        private java.math.BigDecimal receivedQuantity;
    }
}
