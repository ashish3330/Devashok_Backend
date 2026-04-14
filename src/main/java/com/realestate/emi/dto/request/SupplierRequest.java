package com.realestate.emi.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SupplierRequest {

    @NotBlank(message = "Supplier name is required")
    private String name;

    private String contactPerson;

    private String phone;

    @Email(message = "Invalid email format")
    private String email;

    private String gstNumber;

    private String address;

    private java.util.List<Long> materialIds;
}
