package com.realestate.emi.mapper;

import com.realestate.emi.dto.response.StockTransactionResponse;
import com.realestate.emi.entity.StockTransaction;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface StockTransactionMapper {

    @Mapping(source = "material.id", target = "materialId")
    @Mapping(source = "material.name", target = "materialName")
    @Mapping(source = "supplier.id", target = "supplierId")
    @Mapping(source = "supplier.name", target = "supplierName")
    @Mapping(source = "deal.id", target = "dealId")
    @Mapping(source = "installmentPhase.id", target = "installmentPhaseId")
    StockTransactionResponse toResponse(StockTransaction transaction);

    List<StockTransactionResponse> toResponseList(List<StockTransaction> transactions);
}
