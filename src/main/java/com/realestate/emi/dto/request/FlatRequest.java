package com.realestate.emi.dto.request;

import com.realestate.emi.enums.FlatType;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
public class FlatRequest {

    @NotNull(message = "Block id is required")
    private Long blockId;

    @NotBlank(message = "Flat number is required")
    @Size(max = 30)
    private String flatNumber;

    private Integer floor;

    private FlatType type;

    @DecimalMin(value = "0.0", inclusive = false, message = "sqft must be positive")
    private BigDecimal sqft;

    @Size(max = 200)
    private String ownerName;

    private Boolean isOccupied;
}
