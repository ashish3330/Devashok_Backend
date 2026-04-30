package com.realestate.emi.service;

import com.realestate.emi.dto.request.ComplaintCategoryRequest;
import com.realestate.emi.dto.response.ComplaintCategoryResponse;
import com.realestate.emi.entity.ComplaintCategory;
import com.realestate.emi.entity.Organization;
import com.realestate.emi.exception.ResourceNotFoundException;
import com.realestate.emi.exception.ServiceException;
import com.realestate.emi.mapper.ComplaintCategoryMapper;
import com.realestate.emi.repository.ComplaintCategoryRepository;
import com.realestate.emi.repository.OrganizationRepository;
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
public class ComplaintCategoryService {

    private final ComplaintCategoryRepository categoryRepository;
    private final ComplaintCategoryMapper categoryMapper;
    private final TenantContext tenantContext;
    private final OrganizationRepository organizationRepository;

    @Transactional(readOnly = true)
    public List<ComplaintCategoryResponse> findAll() {
        Long orgId = tenantContext.getCurrentOrganizationId();
        return categoryRepository.findByOrganizationIdOrderByNameAsc(orgId).stream()
                .map(categoryMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ComplaintCategoryResponse findById(Long id) {
        Long orgId = tenantContext.getCurrentOrganizationId();
        ComplaintCategory category = categoryRepository.findByIdAndOrganizationId(id, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("ComplaintCategory", id));
        return categoryMapper.toResponse(category);
    }

    @Transactional
    public ComplaintCategoryResponse create(ComplaintCategoryRequest request) {
        Long orgId = tenantContext.getCurrentOrganizationId();
        if (categoryRepository.existsByOrganizationIdAndName(orgId, request.getName())) {
            throw new ServiceException("Category '" + request.getName() + "' already exists", "DUPLICATE_CATEGORY");
        }
        ComplaintCategory category = categoryMapper.toEntity(request);
        Organization org = organizationRepository.findById(orgId)
                .orElseThrow(() -> new ServiceException("Organization not found", "ORG_NOT_FOUND"));
        category.setOrganization(org);
        if (request.getIsActive() != null) {
            category.setIsActive(request.getIsActive());
        }
        ComplaintCategory saved = categoryRepository.save(category);
        log.info("Created complaint category id={} name={}", saved.getId(), saved.getName());
        return categoryMapper.toResponse(saved);
    }

    @Transactional
    public ComplaintCategoryResponse update(Long id, ComplaintCategoryRequest request) {
        Long orgId = tenantContext.getCurrentOrganizationId();
        ComplaintCategory existing = categoryRepository.findByIdAndOrganizationId(id, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("ComplaintCategory", id));
        if (!existing.getName().equals(request.getName())
                && categoryRepository.existsByOrganizationIdAndName(orgId, request.getName())) {
            throw new ServiceException("Category '" + request.getName() + "' already exists", "DUPLICATE_CATEGORY");
        }
        categoryMapper.updateEntityFromRequest(request, existing);
        if (request.getIsActive() != null) {
            existing.setIsActive(request.getIsActive());
        }
        ComplaintCategory saved = categoryRepository.save(existing);
        return categoryMapper.toResponse(saved);
    }

    @Transactional
    public void delete(Long id) {
        Long orgId = tenantContext.getCurrentOrganizationId();
        ComplaintCategory existing = categoryRepository.findByIdAndOrganizationId(id, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("ComplaintCategory", id));
        categoryRepository.delete(existing);
    }
}
