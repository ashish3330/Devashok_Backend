package com.realestate.emi.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpcomingEmiWindow {

    private long count;
    private BigDecimal totalDueAmount;
    private List<UpcomingEmiItem> items;
}
