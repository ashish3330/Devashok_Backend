package com.realestate.emi.service;

import com.realestate.emi.dto.request.FlatRequest;
import com.realestate.emi.dto.response.FlatResponse;
import com.realestate.emi.entity.Block;
import com.realestate.emi.entity.Flat;
import com.realestate.emi.entity.Organization;
import com.realestate.emi.exception.ResourceNotFoundException;
import com.realestate.emi.exception.ServiceException;
import com.realestate.emi.mapper.FlatMapper;
import com.realestate.emi.repository.BlockRepository;
import com.realestate.emi.repository.FlatRepository;
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
public class FlatService {

    private final FlatRepository flatRepository;
    private final BlockRepository blockRepository;
    private final FlatMapper flatMapper;
    private final TenantContext tenantContext;
    private final OrganizationRepository organizationRepository;

    @Transactional(readOnly = true)
    public List<FlatResponse> findAll(Long blockId) {
        Long orgId = tenantContext.getCurrentOrganizationId();
        List<Flat> flats = (blockId != null)
                ? flatRepository.findByOrganizationIdAndBlockIdOrderByFlatNumberAsc(orgId, blockId)
                : flatRepository.findByOrganizationIdOrderByFlatNumberAsc(orgId);
        return flats.stream().map(flatMapper::toResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public FlatResponse findById(Long id) {
        Long orgId = tenantContext.getCurrentOrganizationId();
        Flat flat = flatRepository.findByIdAndOrganizationId(id, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Flat", id));
        return flatMapper.toResponse(flat);
    }

    @Transactional
    public FlatResponse create(FlatRequest request) {
        Long orgId = tenantContext.getCurrentOrganizationId();
        Block block = blockRepository.findByIdAndOrganizationId(request.getBlockId(), orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Block", request.getBlockId()));

        if (flatRepository.existsByOrganizationIdAndBlockIdAndFlatNumber(orgId, request.getBlockId(), request.getFlatNumber())) {
            throw new ServiceException("Flat '" + request.getFlatNumber() + "' already exists in this block", "DUPLICATE_FLAT");
        }

        Flat flat = flatMapper.toEntity(request);
        Organization org = organizationRepository.findById(orgId)
                .orElseThrow(() -> new ServiceException("Organization not found", "ORG_NOT_FOUND"));
        flat.setOrganization(org);
        flat.setBlock(block);
        if (request.getIsOccupied() != null) {
            flat.setIsOccupied(request.getIsOccupied());
        }
        Flat saved = flatRepository.save(flat);
        log.info("Created flat id={} number={}", saved.getId(), saved.getFlatNumber());
        return flatMapper.toResponse(saved);
    }

    @Transactional
    public FlatResponse update(Long id, FlatRequest request) {
        Long orgId = tenantContext.getCurrentOrganizationId();
        Flat existing = flatRepository.findByIdAndOrganizationId(id, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Flat", id));

        Block block = blockRepository.findByIdAndOrganizationId(request.getBlockId(), orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Block", request.getBlockId()));

        boolean blockChanged = !existing.getBlock().getId().equals(request.getBlockId());
        boolean numberChanged = !existing.getFlatNumber().equals(request.getFlatNumber());
        if ((blockChanged || numberChanged)
                && flatRepository.existsByOrganizationIdAndBlockIdAndFlatNumberAndIdNot(
                        orgId, request.getBlockId(), request.getFlatNumber(), id)) {
            throw new ServiceException("Flat '" + request.getFlatNumber() + "' already exists in this block", "DUPLICATE_FLAT");
        }

        flatMapper.updateEntityFromRequest(request, existing);
        existing.setBlock(block);
        if (request.getIsOccupied() != null) {
            existing.setIsOccupied(request.getIsOccupied());
        }
        Flat saved = flatRepository.save(existing);
        return flatMapper.toResponse(saved);
    }

    @Transactional
    public void delete(Long id) {
        Long orgId = tenantContext.getCurrentOrganizationId();
        Flat existing = flatRepository.findByIdAndOrganizationId(id, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Flat", id));
        flatRepository.delete(existing);
    }
}
