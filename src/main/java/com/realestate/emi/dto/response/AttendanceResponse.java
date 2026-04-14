package com.realestate.emi.dto.response;

import com.realestate.emi.enums.AttendanceStatus;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttendanceResponse {

    private Long id;
    private Long staffId;
    private String staffName;
    private String staffRole;
    private LocalDate date;
    private AttendanceStatus status;
    private LocalTime checkIn;
    private LocalTime checkOut;
    private Double overtimeHours;
    private String remarks;
    private String markedBy;
}
