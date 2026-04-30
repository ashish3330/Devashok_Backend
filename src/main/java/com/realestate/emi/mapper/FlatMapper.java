package com.realestate.emi.mapper;

import com.realestate.emi.dto.request.FlatRequest;
import com.realestate.emi.dto.response.FlatResponse;
import com.realestate.emi.entity.Flat;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring")
public interface FlatMapper {

    @Mapping(target = "blockId", source = "block.id")
    @Mapping(target = "blockName", source = "block.name")
    @Mapping(target = "blockCode", source = "block.code")
    FlatResponse toResponse(Flat flat);

    @Mapping(target = "block", ignore = true)
    @Mapping(target = "organization", ignore = true)
    Flat toEntity(FlatRequest request);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "block", ignore = true)
    @Mapping(target = "organization", ignore = true)
    void updateEntityFromRequest(FlatRequest request, @MappingTarget Flat flat);
}
