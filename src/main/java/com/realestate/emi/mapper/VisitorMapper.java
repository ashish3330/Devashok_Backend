package com.realestate.emi.mapper;

import com.realestate.emi.dto.response.VisitorResponse;
import com.realestate.emi.entity.Visitor;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface VisitorMapper {

    @Mapping(target = "flatId", source = "flat.id")
    @Mapping(target = "flatNumber", source = "flat.flatNumber")
    @Mapping(target = "blockName", source = "flat.block.name")
    @Mapping(target = "residentId", source = "resident.id")
    @Mapping(target = "residentName", source = "resident.fullName")
    VisitorResponse toResponse(Visitor visitor);
}
