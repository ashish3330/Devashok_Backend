package com.realestate.emi.mapper;

import com.realestate.emi.dto.request.VehicleRequest;
import com.realestate.emi.dto.response.VehicleResponse;
import com.realestate.emi.entity.Vehicle;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface VehicleMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "organization", ignore = true)
    @Mapping(target = "resident", ignore = true)
    @Mapping(target = "isActive", ignore = true)
    Vehicle toEntity(VehicleRequest request);

    @Mapping(target = "residentId", source = "resident.id")
    @Mapping(target = "residentName", source = "resident.fullName")
    VehicleResponse toResponse(Vehicle vehicle);

    List<VehicleResponse> toResponseList(List<Vehicle> vehicles);
}
