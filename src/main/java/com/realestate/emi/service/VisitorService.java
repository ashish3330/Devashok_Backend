package com.realestate.emi.service;

import com.realestate.emi.dto.request.VisitorPreApproveRequest;
import com.realestate.emi.dto.response.VisitorResponse;
import com.realestate.emi.entity.Flat;
import com.realestate.emi.entity.Organization;
import com.realestate.emi.entity.Resident;
import com.realestate.emi.entity.Visitor;
import com.realestate.emi.enums.VisitorStatus;
import com.realestate.emi.exception.ResourceNotFoundException;
import com.realestate.emi.exception.ServiceException;
import com.realestate.emi.mapper.VisitorMapper;
import com.realestate.emi.repository.FlatRepository;
import com.realestate.emi.repository.OrganizationRepository;
import com.realestate.emi.repository.ResidentRepository;
import com.realestate.emi.repository.VisitorRepository;
import com.realestate.emi.security.TenantContext;
import com.realestate.emi.specification.DateRangeSpec;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class VisitorService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final VisitorRepository visitorRepository;
    private final VisitorMapper visitorMapper;
    private final TenantContext tenantContext;
    private final OrganizationRepository organizationRepository;
    private final FlatRepository flatRepository;
    private final ResidentRepository residentRepository;

    @Transactional(readOnly = true)
    public List<VisitorResponse> listAdmin(Long blockId, Long flatId, VisitorStatus status,
                                           LocalDate from, LocalDate to) {
        Long orgId = tenantContext.getCurrentOrganizationId();
        Specification<Visitor> spec = (root, query, cb) -> {
            List<Predicate> ps = new ArrayList<>();
            ps.add(cb.equal(root.get("organization").get("id"), orgId));
            if (flatId != null) ps.add(cb.equal(root.get("flat").get("id"), flatId));
            if (blockId != null) ps.add(cb.equal(root.get("flat").get("block").get("id"), blockId));
            if (status != null) ps.add(cb.equal(root.get("status"), status));
            return cb.and(ps.toArray(new Predicate[0]));
        };
        spec = spec.and(DateRangeSpec.dateRange("expectedAt", from, to));
        return visitorRepository.findAll(spec).stream()
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .map(visitorMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public VisitorResponse adminCheckIn(Long id) {
        Long orgId = tenantContext.getCurrentOrganizationId();
        Visitor v = visitorRepository.findByIdAndOrganizationId(id, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Visitor", id));
        if (v.getStatus() == VisitorStatus.CHECKED_OUT || v.getStatus() == VisitorStatus.CANCELLED
                || v.getStatus() == VisitorStatus.EXPIRED) {
            throw new ServiceException("Cannot check in a " + v.getStatus() + " visitor", "INVALID_VISITOR_STATE");
        }
        v.setStatus(VisitorStatus.CHECKED_IN);
        v.setCheckedInAt(LocalDateTime.now());
        return visitorMapper.toResponse(visitorRepository.save(v));
    }

    @Transactional
    public VisitorResponse adminCheckOut(Long id) {
        Long orgId = tenantContext.getCurrentOrganizationId();
        Visitor v = visitorRepository.findByIdAndOrganizationId(id, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Visitor", id));
        if (v.getStatus() != VisitorStatus.CHECKED_IN) {
            throw new ServiceException("Visitor is not currently checked in", "INVALID_VISITOR_STATE");
        }
        v.setStatus(VisitorStatus.CHECKED_OUT);
        v.setCheckedOutAt(LocalDateTime.now());
        return visitorMapper.toResponse(visitorRepository.save(v));
    }

    @Transactional(readOnly = true)
    public List<VisitorResponse> listForResident() {
        Long orgId = tenantContext.getCurrentOrganizationId();
        Long flatId = tenantContext.getCurrentFlatId();
        if (flatId == null) {
            throw new ServiceException("Resident flat context missing", "RESIDENT_CONTEXT_MISSING");
        }
        return visitorRepository.findByOrganizationIdAndFlatIdOrderByExpectedAtDesc(orgId, flatId).stream()
                .map(visitorMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public VisitorResponse residentPreApprove(VisitorPreApproveRequest request) {
        Long orgId = tenantContext.getCurrentOrganizationId();
        Long flatId = tenantContext.getCurrentFlatId();
        Long residentId = tenantContext.getCurrentResidentId();
        if (flatId == null || residentId == null) {
            throw new ServiceException("Resident flat context missing", "RESIDENT_CONTEXT_MISSING");
        }
        Organization org = organizationRepository.findById(orgId)
                .orElseThrow(() -> new ServiceException("Organization not found", "ORG_NOT_FOUND"));
        Flat flat = flatRepository.findByIdAndOrganizationId(flatId, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Flat", flatId));
        Resident resident = residentRepository.findByIdAndOrganizationId(residentId, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Resident", residentId));

        Visitor visitor = Visitor.builder()
                .organization(org)
                .flat(flat)
                .resident(resident)
                .name(request.getName())
                .phone(request.getPhone())
                .purpose(request.getPurpose())
                .vehicleNo(request.getVehicleNo())
                .expectedAt(request.getExpectedAt())
                .otp(generateGateCode())
                .status(VisitorStatus.PRE_APPROVED)
                .build();
        Visitor saved = visitorRepository.save(visitor);
        log.info("Resident pre-approved visitor id={} name={} flat={}", saved.getId(), saved.getName(), flatId);
        return visitorMapper.toResponse(saved);
    }

    @Transactional
    public VisitorResponse residentCancel(Long id) {
        Long orgId = tenantContext.getCurrentOrganizationId();
        Long flatId = tenantContext.getCurrentFlatId();
        Visitor v = visitorRepository.findByIdAndOrganizationIdAndFlatId(id, orgId, flatId)
                .orElseThrow(() -> new ResourceNotFoundException("Visitor", id));
        if (v.getStatus() != VisitorStatus.PRE_APPROVED) {
            throw new ServiceException("Only pre-approved visitors can be cancelled", "INVALID_VISITOR_STATE");
        }
        v.setStatus(VisitorStatus.CANCELLED);
        return visitorMapper.toResponse(visitorRepository.save(v));
    }

    private String generateGateCode() {
        int code = 100000 + RANDOM.nextInt(900000);
        return String.valueOf(code);
    }
}
