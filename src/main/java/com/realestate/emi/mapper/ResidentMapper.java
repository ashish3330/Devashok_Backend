package com.realestate.emi.mapper;

import com.realestate.emi.dto.request.ResidentRequest;
import com.realestate.emi.dto.response.ResidentPhoneResponse;
import com.realestate.emi.dto.response.ResidentResponse;
import com.realestate.emi.entity.Resident;
import com.realestate.emi.entity.ResidentPhone;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring")
public interface ResidentMapper {

    @Mapping(target = "flatId", source = "flat.id")
    @Mapping(target = "flatNumber", source = "flat.flatNumber")
    @Mapping(target = "blockId", source = "flat.block.id")
    @Mapping(target = "blockName", source = "flat.block.name")
    @Mapping(target = "phones", ignore = true)
    ResidentResponse toResponse(Resident resident);

    @Mapping(target = "flat", ignore = true)
    @Mapping(target = "organization", ignore = true)
    @Mapping(target = "isActive", ignore = true)
    Resident toEntity(ResidentRequest request);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "flat", ignore = true)
    @Mapping(target = "organization", ignore = true)
    @Mapping(target = "isActive", ignore = true)
    void updateEntityFromRequest(ResidentRequest request, @MappingTarget Resident resident);

    ResidentPhoneResponse toPhoneResponse(ResidentPhone phone);
}
