package com.realestate.emi.service;

import com.realestate.emi.dto.request.SalaryAdvanceRequest;
import com.realestate.emi.dto.response.SalaryAdvanceResponse;
import com.realestate.emi.entity.Organization;
import com.realestate.emi.entity.SalaryAdvance;
import com.realestate.emi.entity.Staff;
import com.realestate.emi.exception.ResourceNotFoundException;
import com.realestate.emi.exception.ServiceException;
import com.realestate.emi.repository.OrganizationRepository;
import com.realestate.emi.repository.SalaryAdvanceRepository;
import com.realestate.emi.repository.StaffRepository;
import com.realestate.emi.security.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SalaryAdvanceService {

    private final SalaryAdvanceRepository salaryAdvanceRepository;
    private final StaffRepository staffRepository;
    private final OrganizationRepository organizationRepository;
    private final TenantContext tenantContext;

    @Transactional
    public SalaryAdvanceResponse grantAdvance(Long staffId, SalaryAdvanceRequest request) {
        Long orgId = tenantContext.getCurrentOrganizationId();

        Staff staff = staffRepository.findById(staffId)
                .orElseThrow(() -> new ResourceNotFoundException("Staff", staffId));
        if (staff.getOrganization() != null && !staff.getOrganization().getId().equals(orgId)) {
            throw new ResourceNotFoundException("Staff", staffId);
        }

        Organization org = organizationRepository.findById(orgId)
                .orElseThrow(() -> new ServiceException("Organization not found", "ORG_NOT_FOUND"));

        String approvedBy = tenantContext.getCurrentUsername();

        SalaryAdvance advance = SalaryAdvance.builder()
                .staff(staff)
                .amount(request.getAmount())
                .balanceRemaining(request.getAmount())
                .advanceDate(request.getAdvanceDate() != null ? request.getAdvanceDate() : LocalDate.now())
                .monthlyDeductionAmount(request.getMonthlyDeductionAmount())
                .status("ACTIVE")
                .remarks(request.getRemarks())
                .approvedBy(approvedBy)
                .organization(org)
                .build();

        advance = salaryAdvanceRepository.save(advance);
        log.info("Granted salary advance of {} to staff {} [{}], approved by {}",
                request.getAmount(), staff.getFullName(), staffId, approvedBy);
        return toResponse(advance);
    }

    @Transactional(readOnly = true)
    public List<SalaryAdvanceResponse> getAdvancesByStaff(Long staffId) {
        Long orgId = tenantContext.getCurrentOrganizationId();

        Staff staff = staffRepository.findById(staffId)
                .orElseThrow(() -> new ResourceNotFoundException("Staff", staffId));
        if (staff.getOrganization() != null && !staff.getOrganization().getId().equals(orgId)) {
            throw new ResourceNotFoundException("Staff", staffId);
        }

        return salaryAdvanceRepository.findByStaffIdAndOrganizationIdOrderByAdvanceDateDesc(staffId, orgId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<SalaryAdvanceResponse> getActiveAdvances() {
        Long orgId = tenantContext.getCurrentOrganizationId();
        return salaryAdvanceRepository.findByOrganizationIdAndStatusOrderByAdvanceDateDesc(orgId, "ACTIVE")
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public SalaryAdvanceResponse cancelAdvance(Long advanceId) {
        Long orgId = tenantContext.getCurrentOrganizationId();

        SalaryAdvance advance = salaryAdvanceRepository.findById(advanceId)
                .orElseThrow(() -> new ResourceNotFoundException("SalaryAdvance", advanceId));
        if (!advance.getOrganization().getId().equals(orgId)) {
            throw new ResourceNotFoundException("SalaryAdvance", advanceId);
        }
        if (!"ACTIVE".equals(advance.getStatus())) {
            throw new ServiceException("Only active advances can be cancelled", "INVALID_STATUS");
        }

        advance.setStatus("CANCELLED");
        advance = salaryAdvanceRepository.save(advance);
        log.info("Cancelled salary advance {} for staff {}", advanceId, advance.getStaff().getFullName());
        return toResponse(advance);
    }

    @Transactional(readOnly = true)
    public BigDecimal getOutstandingBalance(Long staffId) {
        Long orgId = tenantContext.getCurrentOrganizationId();
        return salaryAdvanceRepository.sumActiveBalanceByStaff(staffId, orgId);
    }

    private SalaryAdvanceResponse toResponse(SalaryAdvance advance) {
        return SalaryAdvanceResponse.builder()
                .id(advance.getId())
                .staffId(advance.getStaff().getId())
                .staffName(advance.getStaff().getFullName())
                .amount(advance.getAmount())
                .balanceRemaining(advance.getBalanceRemaining())
                .monthlyDeductionAmount(advance.getMonthlyDeductionAmount())
                .advanceDate(advance.getAdvanceDate())
                .status(advance.getStatus())
                .remarks(advance.getRemarks())
                .approvedBy(advance.getApprovedBy())
                .createdAt(advance.getCreatedAt())
                .build();
    }
}
