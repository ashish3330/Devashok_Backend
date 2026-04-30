package com.realestate.emi.dto.response;

import com.realestate.emi.enums.DailyHelpType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DailyHelpAssignmentResponse {

    private Long id;
    private Long helpId;
    private String helpName;
    private DailyHelpType helpType;
    private String helpPhone;
    private String helpPhotoUrl;
    private Long flatId;
    private String flatNumber;
    private LocalDate startDate;
    private LocalDate endDate;
    private BigDecimal monthlySalary;
    private Boolean isActive;
}
