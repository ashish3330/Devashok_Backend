package com.realestate.emi.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class PropertyTypeRequest {

    @NotBlank(message = "Property type name is required")
    @Size(max = 100, message = "Property type name must not exceed 100 characters")
    private String name;

    private String description;
}
