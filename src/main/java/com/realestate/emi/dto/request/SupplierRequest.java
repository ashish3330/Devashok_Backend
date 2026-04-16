package com.realestate.emi.dto.request;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SupplierRequest {

    @NotBlank(message = "Supplier name is required")
    private String name;

    private String contactPerson;

    @Pattern(regexp = "^[6-9]\\d{9}$", message = "Invalid phone number")
    private String phone;

    @Email(message = "Invalid email format")
    private String email;

    @Pattern(regexp = "^[0-9]{2}[A-Z]{5}[0-9]{4}[A-Z]{1}[1-9A-Z]{1}Z[0-9A-Z]{1}$", message = "Invalid GST number")
    private String gstNumber;

    private String address;

    private java.util.List<Long> materialIds;
}
