package com.realestate.emi.dto.request;

import com.realestate.emi.enums.DealStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class DealStatusRequest {

    @NotNull(message = "Status is required")
    private DealStatus status;
}
