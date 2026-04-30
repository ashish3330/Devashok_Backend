package com.realestate.emi.dto.request;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class BlockRequest {

    @NotBlank(message = "Block name is required")
    @Size(max = 100)
    private String name;

    @NotBlank(message = "Block code is required")
    @Size(max = 30)
    private String code;

    @Min(value = 1, message = "Total floors must be at least 1")
    private Integer totalFloors;

    private Boolean isActive;
}
