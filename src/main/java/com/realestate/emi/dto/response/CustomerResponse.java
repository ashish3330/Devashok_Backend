package com.realestate.emi.dto.response;

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
public class CustomerResponse {

    private Long id;
    private String fullName;
    private String phoneNumber;
    private String email;
    private String address;
    private String aadharNumber;
    private String panNumber;
    private LocalDateTime createdAt;
}
