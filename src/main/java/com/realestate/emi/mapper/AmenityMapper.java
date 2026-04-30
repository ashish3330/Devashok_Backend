package com.realestate.emi.mapper;

import com.realestate.emi.dto.request.AmenityRequest;
import com.realestate.emi.dto.response.AmenityBookingResponse;
import com.realestate.emi.dto.response.AmenityResponse;
import com.realestate.emi.entity.Amenity;
import com.realestate.emi.entity.AmenityBooking;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring")
public interface AmenityMapper {

    AmenityResponse toResponse(Amenity amenity);

    @Mapping(target = "organization", ignore = true)
    @Mapping(target = "isActive", ignore = true)
    Amenity toEntity(AmenityRequest request);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "organization", ignore = true)
    @Mapping(target = "isActive", ignore = true)
    void updateEntityFromRequest(AmenityRequest request, @MappingTarget Amenity amenity);

    @Mapping(target = "amenityId", source = "amenity.id")
    @Mapping(target = "amenityName", source = "amenity.name")
    @Mapping(target = "flatId", source = "flat.id")
    @Mapping(target = "flatNumber", source = "flat.flatNumber")
    @Mapping(target = "residentId", source = "resident.id")
    @Mapping(target = "residentName", source = "resident.fullName")
    AmenityBookingResponse toBookingResponse(AmenityBooking booking);
}
