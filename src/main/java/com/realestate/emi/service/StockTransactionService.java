package com.realestate.emi.service;

import com.realestate.emi.dto.request.StockTransactionRequest;
import com.realestate.emi.dto.response.StockTransactionResponse;
import com.realestate.emi.entity.Deal;
import com.realestate.emi.entity.InstallmentPhase;
import com.realestate.emi.entity.Material;
import com.realestate.emi.entity.Organization;
import com.realestate.emi.entity.StockTransaction;
import com.realestate.emi.entity.Supplier;
import com.realestate.emi.enums.TransactionType;
import com.realestate.emi.exception.ResourceNotFoundException;
import com.realestate.emi.exception.ServiceException;
import com.realestate.emi.mapper.StockTransactionMapper;
import com.realestate.emi.repository.DealRepository;
import com.realestate.emi.repository.InstallmentPhaseRepository;
import com.realestate.emi.repository.MaterialRepository;
import com.realestate.emi.repository.OrganizationRepository;
import com.realestate.emi.repository.StockTransactionRepository;
import com.realestate.emi.repository.SupplierRepository;
import com.realestate.emi.security.TenantContext;
import com.realestate.emi.security.CustomPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class StockTransactionService {

    private final StockTransactionRepository transactionRepository;
    private final MaterialRepository materialRepository;
    private final DealRepository dealRepository;
    private final InstallmentPhaseRepository installmentPhaseRepository;
    private final MaterialService materialService;
    private final StockTransactionMapper transactionMapper;
    private final TenantContext tenantContext;
    private final OrganizationRepository organizationRepository;
    private final SupplierRepository supplierRepository;

    @Transactional
    public StockTransactionResponse recordTransaction(StockTransactionRequest request) {
        Material material = materialRepository.findById(request.getMaterialId())
                .orElseThrow(() -> new ResourceNotFoundException("Material", request.getMaterialId()));

        Long orgId = tenantContext.getCurrentOrganizationId();
        if (material.getOrganization() != null && !material.getOrganization().getId().equals(orgId)) {
            throw new ResourceNotFoundException("Material", request.getMaterialId());
        }

        Deal deal = null;
        if (request.getDealId() != null) {
            deal = dealRepository.findById(request.getDealId())
                    .orElseThrow(() -> new ResourceNotFoundException("Deal", request.getDealId()));
            if (deal.getOrganization() != null && !deal.getOrganization().getId().equals(orgId)) {
                throw new ResourceNotFoundException("Deal", request.getDealId());
            }
        }

        InstallmentPhase phase = null;
        if (request.getInstallmentPhaseId() != null) {
            phase = installmentPhaseRepository.findById(request.getInstallmentPhaseId())
                    .orElseThrow(() -> new ResourceNotFoundException("InstallmentPhase", request.getInstallmentPhaseId()));
            if (phase.getDeal() != null && phase.getDeal().getOrganization() != null
                    && !phase.getDeal().getOrganization().getId().equals(orgId)) {
                throw new ResourceNotFoundException("InstallmentPhase", request.getInstallmentPhaseId());
            }
        }

        // Supplier is required for RETURN_TO_SUPPLIER, optional for INWARD/DAMAGE/WASTAGE
        if (request.getType() == TransactionType.RETURN_TO_SUPPLIER && request.getSupplierId() == null) {
            throw new ServiceException("Supplier is required for RETURN_TO_SUPPLIER transactions", "SUPPLIER_REQUIRED");
        }

        Supplier supplier = null;
        if (request.getSupplierId() != null && (
                request.getType() == TransactionType.INWARD
                || request.getType() == TransactionType.RETURN_TO_SUPPLIER
                || request.getType() == TransactionType.DAMAGE
                || request.getType() == TransactionType.WASTAGE)) {
            supplier = supplierRepository.findById(request.getSupplierId())
                    .orElseThrow(() -> new ResourceNotFoundException("Supplier", request.getSupplierId()));
            if (supplier.getOrganization() != null && !supplier.getOrganization().getId().equals(orgId)) {
                throw new ResourceNotFoundException("Supplier", request.getSupplierId());
            }
        }

        BigDecimal unitCost = request.getUnitCost() != null ? request.getUnitCost() : material.getUnitCost();
        BigDecimal totalCost = unitCost.multiply(request.getQuantity());

        switch (request.getType()) {
            case INWARD:
                material.setCurrentQuantity(material.getCurrentQuantity().add(request.getQuantity()));
                break;
            case OUTWARD:
                if (material.getCurrentQuantity().compareTo(request.getQuantity()) < 0) {
                    throw new ServiceException(
                            "Insufficient stock. Available: " + material.getCurrentQuantity() + " " + material.getUnit(),
                            "INSUFFICIENT_STOCK");
                }
                material.setCurrentQuantity(material.getCurrentQuantity().subtract(request.getQuantity()));
                break;
            case WASTAGE:
            case DAMAGE:
                if (material.getCurrentQuantity().compareTo(request.getQuantity()) < 0) {
                    throw new ServiceException(
                            "Insufficient stock. Available: " + material.getCurrentQuantity() + " " + material.getUnit(),
                            "INSUFFICIENT_STOCK");
                }
                material.setCurrentQuantity(material.getCurrentQuantity().subtract(request.getQuantity()));
                break;
            case RETURN_TO_SUPPLIER:
                if (material.getCurrentQuantity().compareTo(request.getQuantity()) < 0) {
                    throw new ServiceException(
                            "Insufficient stock. Available: " + material.getCurrentQuantity() + " " + material.getUnit(),
                            "INSUFFICIENT_STOCK");
                }
                material.setCurrentQuantity(material.getCurrentQuantity().subtract(request.getQuantity()));
                break;
            case ADJUSTMENT:
                material.setCurrentQuantity(request.getQuantity());
                totalCost = BigDecimal.ZERO;
                break;
        }

        materialRepository.save(material);

        Organization org = organizationRepository.findById(orgId)
                .orElseThrow(() -> new ServiceException("Organization not found", "ORG_NOT_FOUND"));

        StockTransaction transaction = StockTransaction.builder()
                .material(material)
                .type(request.getType())
                .quantity(request.getQuantity())
                .unitCostAtTime(unitCost)
                .totalCost(totalCost)
                .supplier(supplier)
                .deal(deal)
                .installmentPhase(phase)
                .referenceNumber(request.getReferenceNumber())
                .remarks(request.getRemarks())
                .transactionDate(LocalDateTime.now())
                .transactedBy(getUsername())
                .build();

        transaction = transactionRepository.save(transaction);

        log.info("{} transaction recorded: {} x {} of material {} (id={})",
                request.getType(), request.getQuantity(), material.getUnit(),
                material.getName(), material.getId());

        // Check for stock alerts
        materialService.checkAndCreateAlerts(material);

        return transactionMapper.toResponse(transaction);
    }

    @Transactional(readOnly = true)
    public List<StockTransactionResponse> getByMaterial(Long materialId) {
        return transactionMapper.toResponseList(
                transactionRepository.findByMaterialIdOrderByTransactionDateDesc(materialId));
    }

    @Transactional(readOnly = true)
    public List<StockTransactionResponse> getByDeal(Long dealId) {
        return transactionMapper.toResponseList(
                transactionRepository.findByDealIdOrderByTransactionDateDesc(dealId));
    }

    @Transactional(readOnly = true)
    public List<StockTransactionResponse> getBySupplier(Long supplierId) {
        return transactionMapper.toResponseList(
                transactionRepository.findBySupplierIdOrderByTransactionDateDesc(supplierId));
    }

    private String getUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof CustomPrincipal principal) {
            return principal.getEmail();
        }
        if (authentication != null) {
            return authentication.getName();
        }
        return "unknown";
    }
}
