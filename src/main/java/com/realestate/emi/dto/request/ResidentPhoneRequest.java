package com.realestate.emi.dto.request;

import com.realestate.emi.enums.ResidentPhoneLabel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ResidentPhoneRequest {

    @NotBlank(message = "Phone is required")
    @Pattern(regexp = "^\\+?[1-9]\\d{9,14}$", message = "Invalid mobile number")
    private String phone;

    @NotNull(message = "Label is required")
    private ResidentPhoneLabel label;
}
