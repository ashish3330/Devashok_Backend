package com.realestate.emi.service;

import com.realestate.emi.dto.request.StaffRoleRequest;
import com.realestate.emi.dto.response.StaffRoleResponse;
import com.realestate.emi.entity.Organization;
import com.realestate.emi.entity.StaffRole;
import com.realestate.emi.exception.ResourceNotFoundException;
import com.realestate.emi.exception.ServiceException;
import com.realestate.emi.repository.OrganizationRepository;
import com.realestate.emi.repository.StaffRoleRepository;
import com.realestate.emi.security.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class StaffRoleService {

    private final StaffRoleRepository staffRoleRepository;
    private final TenantContext tenantContext;
    private final OrganizationRepository organizationRepository;

    @Transactional(readOnly = true)
    public List<StaffRoleResponse> findAll() {
        Long orgId = tenantContext.getCurrentOrganizationId();
        return staffRoleRepository.findByOrganizationIdAndIsActiveTrueOrderByNameAsc(orgId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public StaffRoleResponse create(StaffRoleRequest request) {
        Long orgId = tenantContext.getCurrentOrganizationId();
        if (staffRoleRepository.findByNameIgnoreCaseAndOrganizationId(request.getName(), orgId).isPresent()) {
            throw new ServiceException("Role with name '" + request.getName() + "' already exists", "DUPLICATE_ROLE");
        }
        Organization org = organizationRepository.findById(orgId)
                .orElseThrow(() -> new ServiceException("Organization not found", "ORG_NOT_FOUND"));

        StaffRole role = StaffRole.builder()
                .name(request.getName().toUpperCase())
                .description(request.getDescription())
                .isActive(true)
                .organization(org)
                .build();
        role = staffRoleRepository.save(role);
        log.info("Created staff role: {}", role.getName());
        return toResponse(role);
    }

    @Transactional
    public StaffRoleResponse update(Long id, StaffRoleRequest request) {
        StaffRole role = staffRoleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("StaffRole", id));
        role.setName(request.getName().toUpperCase());
        role.setDescription(request.getDescription());
        role = staffRoleRepository.save(role);
        log.info("Updated staff role: {}", role.getName());
        return toResponse(role);
    }

    @Transactional
    public void delete(Long id) {
        StaffRole role = staffRoleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("StaffRole", id));
        role.setIsActive(false);
        staffRoleRepository.save(role);
        log.info("Soft-deleted staff role: {}", role.getName());
    }

    private StaffRoleResponse toResponse(StaffRole role) {
        return StaffRoleResponse.builder()
                .id(role.getId())
                .name(role.getName())
                .description(role.getDescription())
                .isActive(role.getIsActive())
                .build();
    }
}
