package com.realestate.emi.mapper;

import com.realestate.emi.dto.request.ComplaintCategoryRequest;
import com.realestate.emi.dto.response.ComplaintCategoryResponse;
import com.realestate.emi.entity.ComplaintCategory;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring")
public interface ComplaintCategoryMapper {

    ComplaintCategoryResponse toResponse(ComplaintCategory category);

    @Mapping(target = "organization", ignore = true)
    @Mapping(target = "isActive", ignore = true)
    ComplaintCategory toEntity(ComplaintCategoryRequest request);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "organization", ignore = true)
    @Mapping(target = "isActive", ignore = true)
    void updateEntityFromRequest(ComplaintCategoryRequest request, @MappingTarget ComplaintCategory category);
}
