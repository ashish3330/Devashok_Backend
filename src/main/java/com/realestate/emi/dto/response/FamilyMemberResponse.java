package com.realestate.emi.dto.response;

import com.realestate.emi.enums.FamilyRelation;
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
public class FamilyMemberResponse {

    private Long id;
    private String fullName;
    private FamilyRelation relation;
    private Integer age;
    private String phone;
    private String email;
    private Boolean isActive;
    private Long residentId;
    private String residentName;
    private String flatNumber;
    private String blockCode;
    private LocalDateTime createdAt;
}
