package com.realestate.emi.service;

import com.realestate.emi.dto.request.ResidentDocumentRequest;
import com.realestate.emi.dto.response.ResidentDocumentResponse;
import com.realestate.emi.entity.Organization;
import com.realestate.emi.entity.ResidentDocument;
import com.realestate.emi.exception.ResourceNotFoundException;
import com.realestate.emi.exception.ServiceException;
import com.realestate.emi.mapper.ResidentDocumentMapper;
import com.realestate.emi.repository.OrganizationRepository;
import com.realestate.emi.repository.ResidentDocumentRepository;
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
public class ResidentDocumentService {

    private final ResidentDocumentRepository documentRepository;
    private final ResidentDocumentMapper documentMapper;
    private final TenantContext tenantContext;
    private final OrganizationRepository organizationRepository;

    @Transactional(readOnly = true)
    public List<ResidentDocumentResponse> findAllAdmin() {
        Long orgId = tenantContext.getCurrentOrganizationId();
        return documentRepository.findByOrganizationIdOrderByCreatedAtDesc(orgId).stream()
                .map(documentMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ResidentDocumentResponse> findResidentFeed() {
        Long orgId = tenantContext.getCurrentOrganizationId();
        return documentRepository.findByOrganizationIdAndVisibleToResidentsTrueOrderByCreatedAtDesc(orgId).stream()
                .map(documentMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ResidentDocumentResponse findById(Long id) {
        Long orgId = tenantContext.getCurrentOrganizationId();
        ResidentDocument doc = documentRepository.findByIdAndOrganizationId(id, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Document", id));
        return documentMapper.toResponse(doc);
    }

    @Transactional(readOnly = true)
    public ResidentDocumentResponse findByIdForResident(Long id) {
        Long orgId = tenantContext.getCurrentOrganizationId();
        ResidentDocument doc = documentRepository.findByIdAndOrganizationId(id, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Document", id));
        if (Boolean.FALSE.equals(doc.getVisibleToResidents())) {
            throw new ResourceNotFoundException("Document", id);
        }
        return documentMapper.toResponse(doc);
    }

    @Transactional
    public ResidentDocumentResponse create(ResidentDocumentRequest request) {
        Long orgId = tenantContext.getCurrentOrganizationId();
        ResidentDocument doc = documentMapper.toEntity(request);
        Organization org = organizationRepository.findById(orgId)
                .orElseThrow(() -> new ServiceException("Organization not found", "ORG_NOT_FOUND"));
        doc.setOrganization(org);
        doc.setUploadedByUserId(tenantContext.getCurrentUserId());
        if (request.getVisibleToResidents() != null) {
            doc.setVisibleToResidents(request.getVisibleToResidents());
        }
        ResidentDocument saved = documentRepository.save(doc);
        log.info("Created resident document id={} title={}", saved.getId(), saved.getTitle());
        return documentMapper.toResponse(saved);
    }

    @Transactional
    public ResidentDocumentResponse update(Long id, ResidentDocumentRequest request) {
        Long orgId = tenantContext.getCurrentOrganizationId();
        ResidentDocument existing = documentRepository.findByIdAndOrganizationId(id, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Document", id));
        documentMapper.updateEntityFromRequest(request, existing);
        if (request.getVisibleToResidents() != null) {
            existing.setVisibleToResidents(request.getVisibleToResidents());
        }
        ResidentDocument saved = documentRepository.save(existing);
        return documentMapper.toResponse(saved);
    }

    @Transactional
    public void delete(Long id) {
        Long orgId = tenantContext.getCurrentOrganizationId();
        ResidentDocument existing = documentRepository.findByIdAndOrganizationId(id, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Document", id));
        documentRepository.delete(existing);
    }
}
