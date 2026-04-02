package com.realestate.emi.mapper;

import com.realestate.emi.dto.request.PropertyTypeRequest;
import com.realestate.emi.dto.response.PropertyTypeResponse;
import com.realestate.emi.entity.PropertyType;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface PropertyTypeMapper {

    PropertyTypeResponse toResponse(PropertyType propertyType);

    PropertyType toEntity(PropertyTypeRequest request);

    void updateEntityFromRequest(PropertyTypeRequest request, @MappingTarget PropertyType propertyType);
}
