package com.realestate.emi.mapper;

import com.realestate.emi.dto.request.NoticeRequest;
import com.realestate.emi.dto.response.NoticeResponse;
import com.realestate.emi.entity.Notice;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;
import org.mapstruct.NullValuePropertyMappingStrategy;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring")
public interface NoticeMapper {

    @Mapping(target = "targetBlockIds", source = "targetBlockIds", qualifiedByName = "stringToIds")
    NoticeResponse toResponse(Notice notice);

    @Mapping(target = "organization", ignore = true)
    @Mapping(target = "postedByUserId", ignore = true)
    @Mapping(target = "targetBlockIds", source = "targetBlockIds", qualifiedByName = "idsToString")
    Notice toEntity(NoticeRequest request);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "organization", ignore = true)
    @Mapping(target = "postedByUserId", ignore = true)
    @Mapping(target = "targetBlockIds", source = "targetBlockIds", qualifiedByName = "idsToString")
    void updateEntityFromRequest(NoticeRequest request, @MappingTarget Notice notice);

    @Named("stringToIds")
    default List<Long> stringToIds(String csv) {
        if (csv == null || csv.isBlank()) return null;
        return Arrays.stream(csv.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(Long::parseLong)
                .collect(Collectors.toList());
    }

    @Named("idsToString")
    default String idsToString(List<Long> ids) {
        if (ids == null || ids.isEmpty()) return null;
        return ids.stream().map(String::valueOf).collect(Collectors.joining(","));
    }
}
