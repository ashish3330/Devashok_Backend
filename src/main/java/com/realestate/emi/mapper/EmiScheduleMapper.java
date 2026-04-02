package com.realestate.emi.mapper;

import com.realestate.emi.dto.response.EmiScheduleResponse;
import com.realestate.emi.entity.EmiSchedule;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

import java.util.List;

@Mapper(componentModel = "spring")
public interface EmiScheduleMapper {

    EmiScheduleResponse toResponse(EmiSchedule emiSchedule);

    List<EmiScheduleResponse> toResponseList(List<EmiSchedule> emiSchedules);

    @AfterMapping
    default void computeRemaining(EmiSchedule source, @MappingTarget EmiScheduleResponse target) {
        if (source.getDueAmount() != null && source.getPaidAmount() != null) {
            target.setRemaining(source.getDueAmount().subtract(source.getPaidAmount()));
        } else if (source.getDueAmount() != null) {
            target.setRemaining(source.getDueAmount());
        }
    }
}
