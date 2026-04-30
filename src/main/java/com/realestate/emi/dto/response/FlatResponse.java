package com.realestate.emi.dto.response;

import com.realestate.emi.enums.FlatType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FlatResponse {

    private Long id;
    private Long blockId;
    private String blockName;
    private String blockCode;
    private String flatNumber;
    private Integer floor;
    private FlatType type;
    private BigDecimal sqft;
    private String ownerName;
    private Boolean isOccupied;
    private LocalDateTime createdAt;
}
