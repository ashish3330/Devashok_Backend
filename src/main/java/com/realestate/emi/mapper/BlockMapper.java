package com.realestate.emi.mapper;

import com.realestate.emi.dto.request.BlockRequest;
import com.realestate.emi.dto.response.BlockResponse;
import com.realestate.emi.entity.Block;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring")
public interface BlockMapper {

    BlockResponse toResponse(Block block);

    Block toEntity(BlockRequest request);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEntityFromRequest(BlockRequest request, @MappingTarget Block block);
}
