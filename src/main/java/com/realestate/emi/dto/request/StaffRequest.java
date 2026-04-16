package com.realestate.emi.dto.request;

import com.realestate.emi.enums.EmploymentType;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
public class StaffRequest {

    @NotBlank(message = "Full name is required")
    private String fullName;

    @Pattern(regexp = "^[6-9]\\d{9}$", message = "Invalid mobile number")
    private String phone;

    @Email(message = "Invalid email address")
    private String email;

    @Size(max = 500, message = "Address must not exceed 500 characters")
    private String address;

    @NotNull(message = "Staff role ID is required")
    private Long staffRoleId;

    @NotNull(message = "Monthly salary is required")
    @DecimalMin(value = "0.01", message = "Salary must be greater than 0")
    private BigDecimal monthlySalary;

    @NotNull(message = "Joining date is required")
    private LocalDate joiningDate;

    private String bankAccountNumber;

    private String ifscCode;

    @Pattern(regexp = "^\\d{12}$", message = "Aadhar must be 12 digits")
    private String aadharNumber;

    @Pattern(regexp = "^[A-Z]{5}[0-9]{4}[A-Z]$", message = "Invalid PAN format")
    private String panNumber;

    private String department;

    private String designation;

    private String emergencyContactName;

    private String emergencyContactPhone;

    private EmploymentType employmentType;

    private String bloodGroup;
}
