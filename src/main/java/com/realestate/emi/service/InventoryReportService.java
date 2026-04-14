package com.realestate.emi.service;

import com.realestate.emi.dto.response.DealMaterialCostResponse;
import com.realestate.emi.dto.response.InventoryValuationResponse;
import com.realestate.emi.dto.response.StockTransactionResponse;
import com.realestate.emi.mapper.MaterialMapper;
import com.realestate.emi.mapper.StockTransactionMapper;
import com.realestate.emi.repository.MaterialRepository;
import com.realestate.emi.repository.OrganizationRepository;
import com.realestate.emi.repository.StockTransactionRepository;
import com.realestate.emi.security.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryReportService {

    private final MaterialRepository materialRepository;
    private final StockTransactionRepository transactionRepository;
    private final MaterialMapper materialMapper;
    private final StockTransactionMapper transactionMapper;
    private final TenantContext tenantContext;
    private final OrganizationRepository organizationRepository;

    @Transactional(readOnly = true)
    public InventoryValuationResponse getInventoryValuation() {
        Long orgId = tenantContext.getCurrentOrganizationId();
        var orgMaterials = materialRepository.findByOrganizationIdAndIsActiveTrueOrderByNameAsc(orgId);
        return InventoryValuationResponse.builder()
                .totalInventoryValue(materialRepository.calculateTotalInventoryValueByOrg(orgId))
                .totalMaterials((long) orgMaterials.size())
                .lowStockCount((long) materialRepository.findLowStockMaterialsByOrg(orgId).size())
                .outOfStockCount((long) materialRepository.findOutOfStockMaterialsByOrg(orgId).size())
                .materials(materialMapper.toResponseList(orgMaterials))
                .build();
    }

    @Transactional(readOnly = true)
    public DealMaterialCostResponse getCostPerDeal(Long dealId) {
        BigDecimal totalCost = transactionRepository.sumMaterialCostByDealId(dealId);
        List<StockTransactionResponse> transactions = transactionMapper.toResponseList(
                transactionRepository.findByDealIdOrderByTransactionDateDesc(dealId));

        return DealMaterialCostResponse.builder()
                .dealId(dealId)
                .totalMaterialCost(totalCost)
                .transactions(transactions)
                .build();
    }
}
