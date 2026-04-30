package com.realestate.emi.dto.response;

import com.realestate.emi.enums.ResidentPhoneLabel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResidentPhoneResponse {

    private Long id;
    private String phone;
    private ResidentPhoneLabel label;
    private Boolean isVerified;
}
