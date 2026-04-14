package com.realestate.emi.mapper;

import com.realestate.emi.dto.request.SupplierRequest;
import com.realestate.emi.dto.response.SupplierResponse;
import com.realestate.emi.entity.Supplier;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.List;

@Mapper(componentModel = "spring")
public interface SupplierMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "isActive", ignore = true)
    Supplier toEntity(SupplierRequest request);

    SupplierResponse toResponse(Supplier supplier);

    List<SupplierResponse> toResponseList(List<Supplier> suppliers);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "isActive", ignore = true)
    void updateEntityFromRequest(SupplierRequest request, @MappingTarget Supplier supplier);
}
