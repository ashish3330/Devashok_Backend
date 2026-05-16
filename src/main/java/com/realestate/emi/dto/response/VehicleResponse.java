package com.realestate.emi.dto.response;

import com.realestate.emi.enums.VehicleType;
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
public class VehicleResponse {

    private Long id;
    private String vehicleNumber;
    private VehicleType vehicleType;
    private String make;
    private String model;
    private String color;
    private Boolean isActive;
    private Long residentId;
    private String residentName;
    private LocalDateTime createdAt;
}
