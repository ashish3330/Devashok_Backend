package com.realestate.emi.mapper;

import com.realestate.emi.dto.response.ResidentNotificationResponse;
import com.realestate.emi.entity.ResidentNotification;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ResidentNotificationMapper {

    @Mapping(target = "isRead", expression = "java(notification.getReadAt() != null)")
    ResidentNotificationResponse toResponse(ResidentNotification notification);
}
