package com.realestate.emi.service;

import com.realestate.emi.dto.request.SupplierRequest;
import com.realestate.emi.dto.response.SupplierResponse;
import com.realestate.emi.entity.Material;
import com.realestate.emi.entity.Organization;
import com.realestate.emi.entity.Supplier;
import com.realestate.emi.exception.ResourceNotFoundException;
import com.realestate.emi.exception.ServiceException;
import com.realestate.emi.repository.MaterialRepository;
import com.realestate.emi.repository.OrganizationRepository;
import com.realestate.emi.repository.SupplierRepository;
import com.realestate.emi.security.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SupplierService {

    private final SupplierRepository supplierRepository;
    private final MaterialRepository materialRepository;
    private final TenantContext tenantContext;
    private final OrganizationRepository organizationRepository;

    @Transactional(readOnly = true)
    public List<SupplierResponse> findAll() {
        Long orgId = tenantContext.getCurrentOrganizationId();
        return supplierRepository.findByOrganizationIdAndIsActiveTrueOrderByNameAsc(orgId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public SupplierResponse findById(Long id) {
        Supplier supplier = supplierRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Supplier", id));
        Long orgId = tenantContext.getCurrentOrganizationId();
        if (supplier.getOrganization() != null && !supplier.getOrganization().getId().equals(orgId)) {
            throw new ResourceNotFoundException("Supplier", id);
        }
        return toResponse(supplier);
    }

    @Transactional
    public SupplierResponse create(SupplierRequest request) {
        Long orgId = tenantContext.getCurrentOrganizationId();
        Organization org = organizationRepository.findById(orgId)
                .orElseThrow(() -> new ServiceException("Organization not found", "ORG_NOT_FOUND"));

        Supplier supplier = Supplier.builder()
                .name(request.getName())
                .contactPerson(request.getContactPerson())
                .phone(request.getPhone())
                .email(request.getEmail())
                .gstNumber(request.getGstNumber())
                .address(request.getAddress())
                .isActive(true)
                .organization(org)
                .build();

        if (request.getMaterialIds() != null && !request.getMaterialIds().isEmpty()) {
            List<Material> materials = materialRepository.findAllById(request.getMaterialIds());
            supplier.setMaterials(materials);
        }

        supplier = supplierRepository.save(supplier);
        log.info("Created supplier: {} with {} materials", supplier.getName(), supplier.getMaterials().size());
        return toResponse(supplier);
    }

    @Transactional
    public SupplierResponse update(Long id, SupplierRequest request) {
        Supplier supplier = supplierRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Supplier", id));
        Long orgId = tenantContext.getCurrentOrganizationId();
        if (supplier.getOrganization() != null && !supplier.getOrganization().getId().equals(orgId)) {
            throw new ResourceNotFoundException("Supplier", id);
        }

        supplier.setName(request.getName());
        supplier.setContactPerson(request.getContactPerson());
        supplier.setPhone(request.getPhone());
        supplier.setEmail(request.getEmail());
        supplier.setGstNumber(request.getGstNumber());
        supplier.setAddress(request.getAddress());

        if (request.getMaterialIds() != null) {
            List<Material> materials = materialRepository.findAllById(request.getMaterialIds());
            supplier.setMaterials(materials);
        }

        supplier = supplierRepository.save(supplier);
        log.info("Updated supplier: {}", supplier.getName());
        return toResponse(supplier);
    }

    @Transactional
    public void delete(Long id) {
        Supplier supplier = supplierRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Supplier", id));
        Long orgId = tenantContext.getCurrentOrganizationId();
        if (supplier.getOrganization() != null && !supplier.getOrganization().getId().equals(orgId)) {
            throw new ResourceNotFoundException("Supplier", id);
        }
        supplier.setIsActive(false);
        supplierRepository.save(supplier);
        log.info("Soft-deleted supplier: {}", supplier.getName());
    }

    private SupplierResponse toResponse(Supplier supplier) {
        List<SupplierResponse.MaterialSummary> materialSummaries = new ArrayList<>();
        if (supplier.getMaterials() != null) {
            materialSummaries = supplier.getMaterials().stream()
                    .map(m -> SupplierResponse.MaterialSummary.builder()
                            .id(m.getId())
                            .name(m.getName())
                            .category(m.getCategory().name())
                            .unit(m.getUnit().name())
                            .build())
                    .collect(Collectors.toList());
        }

        return SupplierResponse.builder()
                .id(supplier.getId())
                .name(supplier.getName())
                .contactPerson(supplier.getContactPerson())
                .phone(supplier.getPhone())
                .email(supplier.getEmail())
                .gstNumber(supplier.getGstNumber())
                .address(supplier.getAddress())
                .isActive(supplier.getIsActive())
                .materials(materialSummaries)
                .build();
    }
}
