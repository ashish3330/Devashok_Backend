package com.realestate.emi.mapper;

import com.realestate.emi.dto.response.MaterialResponse;
import com.realestate.emi.entity.Material;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring")
public interface MaterialMapper {

    @Mapping(target = "suppliers", expression = "java(mapSuppliers(material))")
    @Mapping(target = "stockValue", expression = "java(material.getCurrentQuantity().multiply(material.getUnitCost()))")
    @Mapping(target = "lowStock", expression = "java(material.getCurrentQuantity().compareTo(material.getReorderLevel()) <= 0)")
    MaterialResponse toResponse(Material material);

    List<MaterialResponse> toResponseList(List<Material> materials);

    default List<MaterialResponse.SupplierSummary> mapSuppliers(Material material) {
        if (material.getSuppliers() == null || material.getSuppliers().isEmpty()) {
            return Collections.emptyList();
        }
        return material.getSuppliers().stream()
                .map(s -> MaterialResponse.SupplierSummary.builder()
                        .id(s.getId())
                        .name(s.getName())
                        .build())
                .collect(Collectors.toList());
    }
}
