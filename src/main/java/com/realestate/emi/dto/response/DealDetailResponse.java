package com.realestate.emi.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.util.List;

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class DealDetailResponse extends DealSummaryResponse {

    private List<EmiScheduleResponse> emiSchedules;
    private List<PaymentResponse> payments;
}
