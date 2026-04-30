package com.realestate.emi.dto.response;

import com.realestate.emi.enums.PaymentMethod;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MaintenancePaymentResponse {

    private Long id;
    private Long billId;
    private Long residentId;
    private String residentName;
    private BigDecimal amount;
    private PaymentMethod paymentMethod;
    private String utrNumber;
    private LocalDate paymentDate;
    private String receiptNumber;
    private LocalDateTime createdAt;
}
