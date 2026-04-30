package com.realestate.emi.mapper;

import com.realestate.emi.dto.request.ResidentDocumentRequest;
import com.realestate.emi.dto.response.ResidentDocumentResponse;
import com.realestate.emi.entity.ResidentDocument;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring")
public interface ResidentDocumentMapper {

    ResidentDocumentResponse toResponse(ResidentDocument document);

    @Mapping(target = "organization", ignore = true)
    @Mapping(target = "uploadedByUserId", ignore = true)
    ResidentDocument toEntity(ResidentDocumentRequest request);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "organization", ignore = true)
    @Mapping(target = "uploadedByUserId", ignore = true)
    void updateEntityFromRequest(ResidentDocumentRequest request, @MappingTarget ResidentDocument document);
}
