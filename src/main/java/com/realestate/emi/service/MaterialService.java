package com.realestate.emi.service;

import com.realestate.emi.dto.request.MaterialRequest;
import com.realestate.emi.dto.response.MaterialResponse;
import com.realestate.emi.dto.response.StockAlertResponse;
import com.realestate.emi.entity.Material;
import com.realestate.emi.entity.Organization;
import com.realestate.emi.entity.StockAlert;
import com.realestate.emi.enums.AlertType;
import com.realestate.emi.enums.MaterialCategory;
import com.realestate.emi.exception.ResourceNotFoundException;
import com.realestate.emi.exception.ServiceException;
import com.realestate.emi.mapper.MaterialMapper;
import com.realestate.emi.repository.MaterialRepository;
import com.realestate.emi.repository.OrganizationRepository;
import com.realestate.emi.repository.StockAlertRepository;
import com.realestate.emi.security.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class MaterialService {

    private final MaterialRepository materialRepository;
    private final StockAlertRepository stockAlertRepository;
    private final MaterialMapper materialMapper;
    private final TenantContext tenantContext;
    private final OrganizationRepository organizationRepository;

    @Transactional(readOnly = true)
    public List<MaterialResponse> findAll() {
        Long orgId = tenantContext.getCurrentOrganizationId();
        return materialMapper.toResponseList(materialRepository.findByOrganizationIdAndIsActiveTrueOrderByNameAsc(orgId));
    }

    @Transactional(readOnly = true)
    public MaterialResponse findById(Long id) {
        Material material = materialRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Material", id));
        Long orgId = tenantContext.getCurrentOrganizationId();
        if (material.getOrganization() != null && !material.getOrganization().getId().equals(orgId)) {
            throw new ResourceNotFoundException("Material", id);
        }
        return materialMapper.toResponse(material);
    }

    @Transactional(readOnly = true)
    public List<MaterialResponse> findByCategory(MaterialCategory category) {
        Long orgId = tenantContext.getCurrentOrganizationId();
        return materialMapper.toResponseList(materialRepository.findByCategoryAndOrganizationIdAndIsActiveTrue(category, orgId));
    }

    @Transactional(readOnly = true)
    public List<MaterialResponse> findLowStock() {
        Long orgId = tenantContext.getCurrentOrganizationId();
        return materialMapper.toResponseList(materialRepository.findLowStockMaterialsByOrg(orgId));
    }

    @Transactional
    public MaterialResponse create(MaterialRequest request) {
        Long orgId = tenantContext.getCurrentOrganizationId();
        Organization org = organizationRepository.findById(orgId)
                .orElseThrow(() -> new ServiceException("Organization not found", "ORG_NOT_FOUND"));

        Material material = Material.builder()
                .name(request.getName())
                .category(request.getCategory())
                .unit(request.getUnit())
                .unitCost(request.getUnitCost())
                .hsnCode(request.getHsnCode())
                .reorderLevel(request.getReorderLevel())
                .minimumThreshold(request.getMinimumThreshold())
                .currentQuantity(BigDecimal.ZERO)
                .isActive(true)
                .organization(org)
                .build();

        material = materialRepository.save(material);
        log.info("Created material: {}", material.getName());
        return materialMapper.toResponse(material);
    }

    @Transactional
    public MaterialResponse update(Long id, MaterialRequest request) {
        Material material = materialRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Material", id));
        Long orgId = tenantContext.getCurrentOrganizationId();
        if (material.getOrganization() != null && !material.getOrganization().getId().equals(orgId)) {
            throw new ResourceNotFoundException("Material", id);
        }

        material.setName(request.getName());
        material.setCategory(request.getCategory());
        material.setUnit(request.getUnit());
        material.setUnitCost(request.getUnitCost());
        material.setHsnCode(request.getHsnCode());
        material.setReorderLevel(request.getReorderLevel());
        material.setMinimumThreshold(request.getMinimumThreshold());

        material = materialRepository.save(material);
        log.info("Updated material: {}", material.getName());
        return materialMapper.toResponse(material);
    }

    @Transactional
    public void delete(Long id) {
        Material material = materialRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Material", id));
        Long orgId = tenantContext.getCurrentOrganizationId();
        if (material.getOrganization() != null && !material.getOrganization().getId().equals(orgId)) {
            throw new ResourceNotFoundException("Material", id);
        }
        material.setIsActive(false);
        materialRepository.save(material);
        log.info("Soft-deleted material: {}", material.getName());
    }

    public void checkAndCreateAlerts(Material material) {
        if (material.getCurrentQuantity().compareTo(BigDecimal.ZERO) == 0) {
            createAlert(material, AlertType.OUT_OF_STOCK);
        } else if (material.getCurrentQuantity().compareTo(material.getReorderLevel()) <= 0) {
            createAlert(material, AlertType.LOW_STOCK);
        }
    }

    private void createAlert(Material material, AlertType type) {
        StockAlert alert = StockAlert.builder()
                .material(material)
                .alertType(type)
                .currentQuantity(material.getCurrentQuantity())
                .threshold(material.getReorderLevel())
                .acknowledged(false)
                .build();
        stockAlertRepository.save(alert);
        log.warn("Stock alert created: {} for material {} (qty: {})",
                type, material.getName(), material.getCurrentQuantity());
    }

    @Transactional(readOnly = true)
    public List<StockAlertResponse> getUnacknowledgedAlerts() {
        Long orgId = tenantContext.getCurrentOrganizationId();
        return stockAlertRepository.findByAcknowledgedFalseAndMaterialOrganizationIdOrderByCreatedAtDesc(orgId).stream()
                .map(alert -> StockAlertResponse.builder()
                        .id(alert.getId())
                        .materialId(alert.getMaterial().getId())
                        .materialName(alert.getMaterial().getName())
                        .alertType(alert.getAlertType())
                        .currentQuantity(alert.getCurrentQuantity())
                        .threshold(alert.getThreshold())
                        .acknowledged(alert.getAcknowledged())
                        .createdAt(alert.getCreatedAt())
                        .build())
                .collect(Collectors.toList());
    }

    @Transactional
    public void acknowledgeAlert(Long alertId, String username) {
        StockAlert alert = stockAlertRepository.findById(alertId)
                .orElseThrow(() -> new ResourceNotFoundException("StockAlert", alertId));
        Long orgId = tenantContext.getCurrentOrganizationId();
        if (alert.getMaterial() != null && alert.getMaterial().getOrganization() != null
                && !alert.getMaterial().getOrganization().getId().equals(orgId)) {
            throw new ResourceNotFoundException("StockAlert", alertId);
        }
        alert.setAcknowledged(true);
        alert.setAcknowledgedBy(username);
        alert.setAcknowledgedAt(java.time.LocalDateTime.now());
        stockAlertRepository.save(alert);
        log.info("Alert {} acknowledged by {}", alertId, username);
    }
}
