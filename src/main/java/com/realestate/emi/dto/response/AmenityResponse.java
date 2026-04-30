package com.realestate.emi.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AmenityResponse {

    private Long id;
    private String name;
    private String description;
    private Integer slotDurationMinutes;
    private LocalTime openTime;
    private LocalTime closeTime;
    private BigDecimal pricePerSlot;
    private Integer capacity;
    private Boolean isActive;
}
