package com.realestate.emi.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VisitorPreApproveRequest {

    @NotBlank
    @Size(max = 200)
    private String name;

    @Size(max = 20)
    private String phone;

    @Size(max = 200)
    private String purpose;

    @Size(max = 30)
    private String vehicleNo;

    @NotNull
    private LocalDateTime expectedAt;
}
