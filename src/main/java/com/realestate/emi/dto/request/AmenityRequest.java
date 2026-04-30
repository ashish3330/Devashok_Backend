package com.realestate.emi.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
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
public class AmenityRequest {

    @NotBlank
    @Size(max = 100)
    private String name;

    private String description;

    @NotNull
    @Min(15)
    private Integer slotDurationMinutes;

    private LocalTime openTime;

    private LocalTime closeTime;

    @DecimalMin(value = "0.0", inclusive = true)
    private BigDecimal pricePerSlot;

    @Min(0)
    private Integer capacity;

    private Boolean isActive;
}
