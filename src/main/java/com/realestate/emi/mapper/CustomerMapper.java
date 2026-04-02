package com.realestate.emi.mapper;

import com.realestate.emi.dto.request.CustomerRequest;
import com.realestate.emi.dto.response.CustomerResponse;
import com.realestate.emi.entity.Customer;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface CustomerMapper {

    CustomerResponse toResponse(Customer customer);

    Customer toEntity(CustomerRequest request);

    void updateEntityFromRequest(CustomerRequest request, @MappingTarget Customer customer);
}
