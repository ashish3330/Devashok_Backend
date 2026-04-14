package com.realestate.emi.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class StaffRoleRequest {

    @NotBlank(message = "Role name is required")
    private String name;

    private String description;
}
