package com.realestate.emi.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
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
        private Long itemId;
        private java.math.BigDecimal receivedQuantity;
    }
}
