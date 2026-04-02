package com.realestate.emi.dto.response;

import com.realestate.emi.enums.PaymentMethod;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentResponse {

    private Long id;
    private BigDecimal amount;
    private LocalDateTime paymentDate;
    private PaymentMethod paymentMethod;
    private String utrNumber;
    private String notes;
    private Long emiScheduleId;
    private LocalDate emiDueDate;
    private String createdByAdmin;
    private LocalDateTime createdAt;
}
