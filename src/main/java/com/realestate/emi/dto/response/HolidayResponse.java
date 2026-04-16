package com.realestate.emi.dto.response;

import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HolidayResponse {

    private Long id;
    private LocalDate date;
    private String name;
    private String type;
    private Boolean isOptional;
}
