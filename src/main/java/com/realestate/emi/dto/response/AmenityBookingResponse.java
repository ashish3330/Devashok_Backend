package com.realestate.emi.dto.response;

import com.realestate.emi.enums.AmenityBookingStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AmenityBookingResponse {

    private Long id;
    private Long amenityId;
    private String amenityName;
    private Long flatId;
    private String flatNumber;
    private Long residentId;
    private String residentName;
    private LocalDate bookingDate;
    private LocalTime slotStart;
    private LocalTime slotEnd;
    private BigDecimal amountPaid;
    private String paymentStatus;
    private AmenityBookingStatus status;
    private String notes;
    private LocalDateTime createdAt;
}
