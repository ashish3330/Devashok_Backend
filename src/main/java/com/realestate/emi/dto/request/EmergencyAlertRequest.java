package com.realestate.emi.dto.request;

import com.realestate.emi.enums.EmergencyType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
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
public class EmergencyAlertRequest {

    @NotNull
    private EmergencyType type;

    @Size(max = 500)
    private String message;

    @Size(max = 200)
    private String location;
}
