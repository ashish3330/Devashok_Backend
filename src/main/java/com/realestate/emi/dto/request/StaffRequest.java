package com.realestate.emi.dto.request;

import com.realestate.emi.enums.EmploymentType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
public class StaffRequest {

    @NotBlank(message = "Full name is required")
    private String fullName;

    private String phone;

    private String email;

    private String address;

    @NotNull(message = "Staff role ID is required")
    private Long staffRoleId;

    @NotNull(message = "Monthly salary is required")
    @DecimalMin(value = "0.00", message = "Monthly salary must be 0 or greater")
    private BigDecimal monthlySalary;

    @NotNull(message = "Joining date is required")
    private LocalDate joiningDate;

    private String bankAccountNumber;

    private String ifscCode;

    private String aadharNumber;

    private String panNumber;

    private String department;

    private String designation;

    private String emergencyContactName;

    private String emergencyContactPhone;

    private EmploymentType employmentType;

    private String bloodGroup;
}
