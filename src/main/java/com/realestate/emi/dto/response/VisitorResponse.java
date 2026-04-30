package com.realestate.emi.dto.response;

import com.realestate.emi.enums.VisitorStatus;
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
public class VisitorResponse {

    private Long id;
    private Long flatId;
    private String flatNumber;
    private String blockName;
    private Long residentId;
    private String residentName;
    private String name;
    private String phone;
    private String purpose;
    private String vehicleNo;
    private LocalDateTime expectedAt;
    private String otp;
    private String photoUrl;
    private VisitorStatus status;
    private LocalDateTime checkedInAt;
    private LocalDateTime checkedOutAt;
    private LocalDateTime createdAt;
}
