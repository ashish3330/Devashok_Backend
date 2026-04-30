package com.realestate.emi.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AmenityBookingRequest {

    @NotNull
    private LocalDate bookingDate;

    @NotNull
    private LocalTime slotStart;

    @NotNull
    private LocalTime slotEnd;

    @Size(max = 1000)
    private String notes;
}
