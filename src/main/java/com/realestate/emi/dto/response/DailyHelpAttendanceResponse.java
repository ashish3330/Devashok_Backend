package com.realestate.emi.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DailyHelpAttendanceResponse {

    private Long id;
    private Long helpId;
    private String helpName;
    private Long flatId;
    private String flatNumber;
    private LocalDateTime checkedInAt;
    private LocalDateTime checkedOutAt;
}
