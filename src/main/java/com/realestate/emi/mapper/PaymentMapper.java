package com.realestate.emi.mapper;

import com.realestate.emi.dto.response.PaymentResponse;
import com.realestate.emi.entity.Payment;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface PaymentMapper {

    @Mapping(source = "emiSchedule.id", target = "emiScheduleId")
    @Mapping(source = "emiSchedule.dueDate", target = "emiDueDate")
    PaymentResponse toResponse(Payment payment);

    List<PaymentResponse> toResponseList(List<Payment> payments);
}
