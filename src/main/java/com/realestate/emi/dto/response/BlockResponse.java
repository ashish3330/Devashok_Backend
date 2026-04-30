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
public class BlockResponse {

    private Long id;
    private String name;
    private String code;
    private Integer totalFloors;
    private Boolean isActive;
    private LocalDateTime createdAt;
}
