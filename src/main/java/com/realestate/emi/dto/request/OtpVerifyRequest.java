package com.realestate.emi.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Getter
public class OtpVerifyRequest {

    @NotBlank
    private String mobile;

    @NotBlank
    private String otp;
}
