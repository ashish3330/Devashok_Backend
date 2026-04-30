package com.realestate.emi.service;

import com.realestate.emi.dto.request.ComplaintRequest;
import com.realestate.emi.dto.request.ComplaintStatusUpdateRequest;
import com.realestate.emi.dto.response.ComplaintResponse;
import com.realestate.emi.entity.*;
import com.realestate.emi.enums.ComplaintStatus;
import com.realestate.emi.exception.ResourceNotFoundException;
import com.realestate.emi.exception.ServiceException;
import com.realestate.emi.mapper.ComplaintMapper;
import com.realestate.emi.repository.*;
import com.realestate.emi.security.TenantContext;
import com.realestate.emi.specification.DateRangeSpec;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ComplaintService {

    private final ComplaintRepository complaintRepository;
    private final ComplaintCategoryRepository categoryRepository;
    private final FlatRepository flatRepository;
    private final ResidentRepository residentRepository;
    private final OrganizationRepository organizationRepository;
    private final ComplaintMapper complaintMapper;
    private final TenantContext tenantContext;

    @Transactional(readOnly = true)
    public List<ComplaintResponse> listAdmin(Long blockId, Long flatId, Long categoryId,
                                             ComplaintStatus status, LocalDate from, LocalDate to) {
        Long orgId = tenantContext.getCurrentOrganizationId();
        Specification<Complaint> spec = (root, query, cb) -> {
            List<Predicate> ps = new ArrayList<>();
            ps.add(cb.equal(root.get("organization").get("id"), orgId));
            if (flatId != null) ps.add(cb.equal(root.get("flat").get("id"), flatId));
            if (blockId != null) ps.add(cb.equal(root.get("flat").get("block").get("id"), blockId));
            if (categoryId != null) ps.add(cb.equal(root.get("category").get("id"), categoryId));
            if (status != null) ps.add(cb.equal(root.get("status"), status));
            return cb.and(ps.toArray(new Predicate[0]));
        };
        spec = spec.and(DateRangeSpec.dateRange("createdAt", from, to));
        return complaintRepository.findAll(spec).stream()
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .map(complaintMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ComplaintResponse findByIdAdmin(Long id) {
        Long orgId = tenantContext.getCurrentOrganizationId();
        Complaint c = complaintRepository.findByIdAndOrganizationId(id, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Complaint", id));
        return complaintMapper.toResponse(c);
    }

    @Transactional
    public ComplaintResponse updateStatus(Long id, ComplaintStatusUpdateRequest request) {
        Long orgId = tenantContext.getCurrentOrganizationId();
        Complaint c = complaintRepository.findByIdAndOrganizationId(id, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Complaint", id));
        c.setStatus(request.getStatus());
        if (request.getResolutionNotes() != null) {
            c.setResolutionNotes(request.getResolutionNotes());
        }
        if (request.getStatus() == ComplaintStatus.RESOLVED || request.getStatus() == ComplaintStatus.CLOSED) {
            if (c.getResolvedAt() == null) {
                c.setResolvedAt(LocalDateTime.now());
            }
        }
        return complaintMapper.toResponse(complaintRepository.save(c));
    }

    @Transactional(readOnly = true)
    public List<ComplaintResponse> listForResident() {
        Long orgId = tenantContext.getCurrentOrganizationId();
        Long flatId = tenantContext.getCurrentFlatId();
        if (flatId == null) {
            throw new ServiceException("Resident flat context missing", "RESIDENT_CONTEXT_MISSING");
        }
        return complaintRepository.findByOrganizationIdAndFlatIdOrderByCreatedAtDesc(orgId, flatId).stream()
                .map(complaintMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ComplaintResponse findByIdForResident(Long id) {
        Long orgId = tenantContext.getCurrentOrganizationId();
        Long flatId = tenantContext.getCurrentFlatId();
        Complaint c = complaintRepository.findByIdAndOrganizationIdAndFlatId(id, orgId, flatId)
                .orElseThrow(() -> new ResourceNotFoundException("Complaint", id));
        return complaintMapper.toResponse(c);
    }

    @Transactional
    public ComplaintResponse residentRaise(ComplaintRequest request) {
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

        ComplaintCategory category = null;
        LocalDateTime slaDeadline = null;
        if (request.getCategoryId() != null) {
            category = categoryRepository.findByIdAndOrganizationId(request.getCategoryId(), orgId)
                    .orElseThrow(() -> new ResourceNotFoundException("ComplaintCategory", request.getCategoryId()));
            if (category.getSlaHours() != null) {
                slaDeadline = LocalDateTime.now().plusHours(category.getSlaHours());
            }
        }

        Complaint c = Complaint.builder()
                .organization(org)
                .flat(flat)
                .resident(resident)
                .category(category)
                .title(request.getTitle())
                .description(request.getDescription())
                .photoUrls(joinPhotos(request.getPhotoUrls()))
                .status(ComplaintStatus.OPEN)
                .slaDeadline(slaDeadline)
                .build();
        Complaint saved = complaintRepository.save(c);
        log.info("Resident raised complaint id={} flat={}", saved.getId(), flatId);
        return complaintMapper.toResponse(saved);
    }

    private String joinPhotos(List<String> urls) {
        if (urls == null || urls.isEmpty()) return null;
        return String.join("||", urls);
    }
}
