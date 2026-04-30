package com.realestate.emi.dto.response;

import com.realestate.emi.enums.ResidentType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResidentResponse {

    private Long id;
    private Long flatId;
    private String flatNumber;
    private Long blockId;
    private String blockName;
    private String fullName;
    private String email;
    private String primaryPhone;
    private ResidentType residentType;
    private LocalDate moveInDate;
    private LocalDate moveOutDate;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private List<ResidentPhoneResponse> phones;
}
