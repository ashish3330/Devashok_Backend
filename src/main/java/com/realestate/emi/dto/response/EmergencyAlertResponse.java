package com.realestate.emi.dto.response;

import com.realestate.emi.enums.EmergencyStatus;
import com.realestate.emi.enums.EmergencyType;
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
public class EmergencyAlertResponse {

    private Long id;
    private EmergencyType type;
    private String message;
    private String location;
    private EmergencyStatus status;
    private Long residentId;
    private String residentName;
    private String flatNumber;
    private String blockCode;
    private String primaryPhone;
    private LocalDateTime createdAt;
    private String acknowledgedByName;
    private LocalDateTime acknowledgedAt;
    private LocalDateTime resolvedAt;
}
