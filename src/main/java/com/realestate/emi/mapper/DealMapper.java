package com.realestate.emi.mapper;

import com.realestate.emi.dto.response.DealSummaryResponse;
import com.realestate.emi.entity.Deal;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface DealMapper {

    @Mapping(source = "customer.id", target = "customerId")
    @Mapping(source = "customer.fullName", target = "customerName")
    @Mapping(source = "propertyType.id", target = "propertyTypeId")
    @Mapping(source = "propertyType.name", target = "propertyTypeName")
    @Mapping(target = "totalPaid", ignore = true)
    @Mapping(target = "outstanding", ignore = true)
    DealSummaryResponse toSummaryResponse(Deal deal);
}
