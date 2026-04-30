package com.realestate.emi.mapper;

import com.realestate.emi.dto.request.DailyHelpRequest;
import com.realestate.emi.dto.response.DailyHelpAssignmentResponse;
import com.realestate.emi.dto.response.DailyHelpAttendanceResponse;
import com.realestate.emi.dto.response.DailyHelpResponse;
import com.realestate.emi.entity.DailyHelp;
import com.realestate.emi.entity.DailyHelpAssignment;
import com.realestate.emi.entity.DailyHelpAttendance;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring")
public interface DailyHelpMapper {

    DailyHelpResponse toResponse(DailyHelp help);

    @Mapping(target = "organization", ignore = true)
    @Mapping(target = "isVerified", ignore = true)
    DailyHelp toEntity(DailyHelpRequest request);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "organization", ignore = true)
    @Mapping(target = "isVerified", ignore = true)
    void updateEntityFromRequest(DailyHelpRequest request, @MappingTarget DailyHelp help);

    @Mapping(target = "helpId", source = "help.id")
    @Mapping(target = "helpName", source = "help.name")
    @Mapping(target = "helpType", source = "help.helpType")
    @Mapping(target = "helpPhone", source = "help.primaryPhone")
    @Mapping(target = "helpPhotoUrl", source = "help.photoUrl")
    @Mapping(target = "flatId", source = "flat.id")
    @Mapping(target = "flatNumber", source = "flat.flatNumber")
    DailyHelpAssignmentResponse toAssignmentResponse(DailyHelpAssignment assignment);

    @Mapping(target = "helpId", source = "help.id")
    @Mapping(target = "helpName", source = "help.name")
    @Mapping(target = "flatId", source = "flat.id")
    @Mapping(target = "flatNumber", source = "flat.flatNumber")
    DailyHelpAttendanceResponse toAttendanceResponse(DailyHelpAttendance attendance);
}
