package com.realestate.emi.dto.response;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StaffResponse {

    private Long id;
    private String fullName;
    private String phone;
    private String email;
    private String address;
    private Long staffRoleId;
    private String staffRoleName;
    private BigDecimal monthlySalary;
    private LocalDate joiningDate;
    private LocalDate exitDate;
    private String bankAccountNumber;
    private String ifscCode;
    private String aadharNumber;
    private Boolean isActive;
}
