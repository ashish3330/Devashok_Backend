package com.realestate.emi.dto.request;

import com.realestate.emi.enums.AttendanceStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Setter
public class AttendanceRequest {

    @NotNull(message = "Staff ID is required")
    private Long staffId;

    @NotNull(message = "Date is required")
    private LocalDate date;

    @NotNull(message = "Status is required")
    private AttendanceStatus status;

    private LocalTime checkIn;

    private LocalTime checkOut;

    private Double overtimeHours = 0.0;

    private String remarks;
}
