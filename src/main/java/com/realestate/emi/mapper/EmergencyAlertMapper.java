package com.realestate.emi.mapper;

import com.realestate.emi.dto.response.EmergencyAlertResponse;
import com.realestate.emi.entity.EmergencyAlert;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface EmergencyAlertMapper {

    @Mapping(target = "residentId", source = "resident.id")
    @Mapping(target = "residentName", expression = "java(alert.getResident() != null ? alert.getResident().getFullName() : null)")
    @Mapping(target = "flatNumber", expression = "java(alert.getFlat() != null ? alert.getFlat().getFlatNumber() : null)")
    @Mapping(target = "blockCode", expression = "java(alert.getFlat() != null && alert.getFlat().getBlock() != null ? alert.getFlat().getBlock().getCode() : null)")
    @Mapping(target = "primaryPhone", expression = "java(alert.getResident() != null ? alert.getResident().getPrimaryPhone() : null)")
    @Mapping(target = "acknowledgedByName", expression = "java(alert.getAcknowledgedBy() != null ? alert.getAcknowledgedBy().getFullName() : null)")
    EmergencyAlertResponse toResponse(EmergencyAlert alert);
}
