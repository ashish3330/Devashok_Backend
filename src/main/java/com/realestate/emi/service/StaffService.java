package com.realestate.emi.service;

import com.realestate.emi.dto.request.StaffRequest;
import com.realestate.emi.dto.response.StaffResponse;
import com.realestate.emi.entity.Organization;
import com.realestate.emi.entity.Staff;
import com.realestate.emi.entity.StaffRole;
import com.realestate.emi.exception.ResourceNotFoundException;
import com.realestate.emi.exception.ServiceException;
import com.realestate.emi.repository.OrganizationRepository;
import com.realestate.emi.repository.StaffRepository;
import com.realestate.emi.repository.StaffRoleRepository;
import com.realestate.emi.security.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class StaffService {

    private final StaffRepository staffRepository;
    private final StaffRoleRepository staffRoleRepository;
    private final TenantContext tenantContext;
    private final OrganizationRepository organizationRepository;

    @Transactional(readOnly = true)
    public List<StaffResponse> findAll() {
        Long orgId = tenantContext.getCurrentOrganizationId();
        return staffRepository.findByOrganizationIdAndIsActiveTrueOrderByFullNameAsc(orgId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public StaffResponse findById(Long id) {
        Staff staff = staffRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Staff", id));
        Long orgId = tenantContext.getCurrentOrganizationId();
        if (staff.getOrganization() != null && !staff.getOrganization().getId().equals(orgId)) {
            throw new ResourceNotFoundException("Staff", id);
        }
        return toResponse(staff);
    }

    @Transactional(readOnly = true)
    public List<StaffResponse> findByRole(Long roleId) {
        return staffRepository.findByStaffRoleIdAndIsActiveTrue(roleId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public StaffResponse create(StaffRequest request) {
        StaffRole role = staffRoleRepository.findById(request.getStaffRoleId())
                .orElseThrow(() -> new ResourceNotFoundException("StaffRole", request.getStaffRoleId()));

        Long orgId = tenantContext.getCurrentOrganizationId();
        Organization org = organizationRepository.findById(orgId)
                .orElseThrow(() -> new ServiceException("Organization not found", "ORG_NOT_FOUND"));

        Staff staff = Staff.builder()
                .fullName(request.getFullName())
                .phone(request.getPhone())
                .email(request.getEmail())
                .address(request.getAddress())
                .staffRole(role)
                .monthlySalary(request.getMonthlySalary())
                .joiningDate(request.getJoiningDate())
                .bankAccountNumber(request.getBankAccountNumber())
                .ifscCode(request.getIfscCode())
                .aadharNumber(request.getAadharNumber())
                .isActive(true)
                .organization(org)
                .build();

        staff = staffRepository.save(staff);
        log.info("Created staff: {} with role {}", staff.getFullName(), role.getName());
        return toResponse(staff);
    }

    @Transactional
    public StaffResponse update(Long id, StaffRequest request) {
        Staff staff = staffRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Staff", id));
        Long orgId = tenantContext.getCurrentOrganizationId();
        if (staff.getOrganization() != null && !staff.getOrganization().getId().equals(orgId)) {
            throw new ResourceNotFoundException("Staff", id);
        }

        StaffRole role = staffRoleRepository.findById(request.getStaffRoleId())
                .orElseThrow(() -> new ResourceNotFoundException("StaffRole", request.getStaffRoleId()));

        staff.setFullName(request.getFullName());
        staff.setPhone(request.getPhone());
        staff.setEmail(request.getEmail());
        staff.setAddress(request.getAddress());
        staff.setStaffRole(role);
        staff.setMonthlySalary(request.getMonthlySalary());
        staff.setJoiningDate(request.getJoiningDate());
        staff.setBankAccountNumber(request.getBankAccountNumber());
        staff.setIfscCode(request.getIfscCode());
        staff.setAadharNumber(request.getAadharNumber());

        staff = staffRepository.save(staff);
        log.info("Updated staff: {}", staff.getFullName());
        return toResponse(staff);
    }

    @Transactional
    public void deactivate(Long id) {
        Staff staff = staffRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Staff", id));
        Long orgId = tenantContext.getCurrentOrganizationId();
        if (staff.getOrganization() != null && !staff.getOrganization().getId().equals(orgId)) {
            throw new ResourceNotFoundException("Staff", id);
        }
        staff.setIsActive(false);
        staff.setExitDate(LocalDate.now());
        staffRepository.save(staff);
        log.info("Deactivated staff: {}", staff.getFullName());
    }

    private StaffResponse toResponse(Staff staff) {
        return StaffResponse.builder()
                .id(staff.getId())
                .fullName(staff.getFullName())
                .phone(staff.getPhone())
                .email(staff.getEmail())
                .address(staff.getAddress())
                .staffRoleId(staff.getStaffRole().getId())
                .staffRoleName(staff.getStaffRole().getName())
                .monthlySalary(staff.getMonthlySalary())
                .joiningDate(staff.getJoiningDate())
                .exitDate(staff.getExitDate())
                .bankAccountNumber(staff.getBankAccountNumber())
                .ifscCode(staff.getIfscCode())
                .aadharNumber(staff.getAadharNumber())
                .isActive(staff.getIsActive())
                .build();
    }
}
