package com.realestate.emi.dto.request;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class CustomerRequest {

    @NotBlank(message = "Full name is required")
    private String fullName;

    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^[6-9]\\d{9}$", message = "Phone number must be a valid 10-digit Indian mobile number")
    private String phoneNumber;

    @Email(message = "Email must be a valid email address")
    private String email;

    @NotBlank(message = "Address is required")
    @Size(max = 500)
    private String address;

    @Pattern(regexp = "^\\d{12}$", message = "Aadhar number must be exactly 12 digits")
    private String aadharNumber;

    @Pattern(regexp = "^[A-Z]{5}[0-9]{4}[A-Z]{1}$", message = "PAN number must be in valid format (e.g., ABCDE1234F)")
    private String panNumber;
}
