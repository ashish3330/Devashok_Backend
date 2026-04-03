package com.realestate.emi.mapper;

import com.realestate.emi.dto.response.EmiScheduleResponse;
import com.realestate.emi.entity.EmiSchedule;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.math.BigDecimal;
import java.util.List;

@Mapper(componentModel = "spring")
public interface EmiScheduleMapper {

    @Mapping(target = "baseEmiAmount", source = "dueAmount")
    @Mapping(target = "totalDueAmount", ignore = true)
    @Mapping(target = "remaining", ignore = true)
    EmiScheduleResponse toResponse(EmiSchedule emiSchedule);

    List<EmiScheduleResponse> toResponseList(List<EmiSchedule> emiSchedules);

    @AfterMapping
    default void computeDerivedFields(EmiSchedule source, @MappingTarget EmiScheduleResponse target) {
        BigDecimal base = source.getDueAmount() != null ? source.getDueAmount() : BigDecimal.ZERO;
        BigDecimal bounce = source.getBounceCharges() != null ? source.getBounceCharges() : BigDecimal.ZERO;
        BigDecimal paid = source.getPaidAmount() != null ? source.getPaidAmount() : BigDecimal.ZERO;

        BigDecimal total = base.add(bounce);
        target.setTotalDueAmount(total);
        target.setRemaining(total.subtract(paid));
    }
}
