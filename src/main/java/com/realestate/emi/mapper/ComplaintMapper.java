package com.realestate.emi.mapper;

import com.realestate.emi.dto.response.ComplaintResponse;
import com.realestate.emi.entity.Complaint;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring")
public interface ComplaintMapper {

    @Mapping(target = "flatId", source = "flat.id")
    @Mapping(target = "flatNumber", source = "flat.flatNumber")
    @Mapping(target = "blockName", source = "flat.block.name")
    @Mapping(target = "residentId", source = "resident.id")
    @Mapping(target = "residentName", source = "resident.fullName")
    @Mapping(target = "categoryId", source = "category.id")
    @Mapping(target = "categoryName", source = "category.name")
    @Mapping(target = "assignedStaffId", source = "assignedStaff.id")
    @Mapping(target = "assignedStaffName", source = "assignedStaff.fullName")
    @Mapping(target = "photoUrls", source = "photoUrls", qualifiedByName = "csvToList")
    ComplaintResponse toResponse(Complaint complaint);

    @Named("csvToList")
    default List<String> csvToList(String csv) {
        if (csv == null || csv.isBlank()) return null;
        return Arrays.stream(csv.split("\\|\\|"))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }
}
