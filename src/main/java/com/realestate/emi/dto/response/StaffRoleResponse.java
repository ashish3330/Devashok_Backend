package com.realestate.emi.dto.response;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StaffRoleResponse {

    private Long id;
    private String name;
    private String description;
    private Boolean isActive;
}
