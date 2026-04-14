package com.realestate.emi.dto.response;

import com.realestate.emi.enums.AlertType;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockAlertResponse {

    private Long id;
    private Long materialId;
    private String materialName;
    private AlertType alertType;
    private BigDecimal currentQuantity;
    private BigDecimal threshold;
    private Boolean acknowledged;
    private String acknowledgedBy;
    private LocalDateTime acknowledgedAt;
    private LocalDateTime createdAt;
}
