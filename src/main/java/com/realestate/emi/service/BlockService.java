package com.realestate.emi.service;

import com.realestate.emi.dto.request.BlockRequest;
import com.realestate.emi.dto.response.BlockResponse;
import com.realestate.emi.entity.Block;
import com.realestate.emi.entity.Organization;
import com.realestate.emi.exception.ResourceNotFoundException;
import com.realestate.emi.exception.ServiceException;
import com.realestate.emi.mapper.BlockMapper;
import com.realestate.emi.repository.BlockRepository;
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
public class BlockService {

    private final BlockRepository blockRepository;
    private final BlockMapper blockMapper;
    private final TenantContext tenantContext;
    private final OrganizationRepository organizationRepository;

    @Transactional(readOnly = true)
    public List<BlockResponse> findAll() {
        Long orgId = tenantContext.getCurrentOrganizationId();
        return blockRepository.findByOrganizationIdOrderByNameAsc(orgId).stream()
                .map(blockMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public BlockResponse findById(Long id) {
        Long orgId = tenantContext.getCurrentOrganizationId();
        Block block = blockRepository.findByIdAndOrganizationId(id, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Block", id));
        return blockMapper.toResponse(block);
    }

    @Transactional
    public BlockResponse create(BlockRequest request) {
        Long orgId = tenantContext.getCurrentOrganizationId();
        if (blockRepository.existsByCodeAndOrganizationId(request.getCode(), orgId)) {
            throw new ServiceException("Block with code '" + request.getCode() + "' already exists", "DUPLICATE_BLOCK_CODE");
        }
        Block block = blockMapper.toEntity(request);
        Organization org = organizationRepository.findById(orgId)
                .orElseThrow(() -> new ServiceException("Organization not found", "ORG_NOT_FOUND"));
        block.setOrganization(org);
        if (request.getIsActive() != null) {
            block.setIsActive(request.getIsActive());
        }
        Block saved = blockRepository.save(block);
        log.info("Created block id={} code={}", saved.getId(), saved.getCode());
        return blockMapper.toResponse(saved);
    }

    @Transactional
    public BlockResponse update(Long id, BlockRequest request) {
        Long orgId = tenantContext.getCurrentOrganizationId();
        Block existing = blockRepository.findByIdAndOrganizationId(id, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Block", id));

        if (!existing.getCode().equals(request.getCode())
                && blockRepository.existsByCodeAndIdNotAndOrganizationId(request.getCode(), id, orgId)) {
            throw new ServiceException("Block with code '" + request.getCode() + "' already exists", "DUPLICATE_BLOCK_CODE");
        }

        blockMapper.updateEntityFromRequest(request, existing);
        if (request.getIsActive() != null) {
            existing.setIsActive(request.getIsActive());
        }
        Block saved = blockRepository.save(existing);
        return blockMapper.toResponse(saved);
    }

    @Transactional
    public void delete(Long id) {
        Long orgId = tenantContext.getCurrentOrganizationId();
        Block existing = blockRepository.findByIdAndOrganizationId(id, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Block", id));
        blockRepository.delete(existing);
    }
}
