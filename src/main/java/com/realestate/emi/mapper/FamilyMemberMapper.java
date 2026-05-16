package com.realestate.emi.mapper;

import com.realestate.emi.dto.request.FamilyMemberRequest;
import com.realestate.emi.dto.response.FamilyMemberResponse;
import com.realestate.emi.entity.FamilyMember;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface FamilyMemberMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "organization", ignore = true)
    @Mapping(target = "resident", ignore = true)
    @Mapping(target = "flat", ignore = true)
    @Mapping(target = "isActive", ignore = true)
    FamilyMember toEntity(FamilyMemberRequest request);

    @Mapping(target = "residentId", expression = "java(member.getResident() != null ? member.getResident().getId() : null)")
    @Mapping(target = "residentName", expression = "java(member.getResident() != null ? member.getResident().getFullName() : null)")
    @Mapping(target = "flatNumber", expression = "java(member.getFlat() != null ? member.getFlat().getFlatNumber() : null)")
    @Mapping(target = "blockCode", expression = "java(member.getFlat() != null && member.getFlat().getBlock() != null ? member.getFlat().getBlock().getCode() : null)")
    FamilyMemberResponse toResponse(FamilyMember member);

    List<FamilyMemberResponse> toResponseList(List<FamilyMember> members);
}
