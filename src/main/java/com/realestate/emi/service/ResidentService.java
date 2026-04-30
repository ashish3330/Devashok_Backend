package com.realestate.emi.service;

import com.realestate.emi.dto.request.ResidentPhoneRequest;
import com.realestate.emi.dto.request.ResidentRequest;
import com.realestate.emi.dto.response.ResidentPhoneResponse;
import com.realestate.emi.dto.response.ResidentResponse;
import com.realestate.emi.entity.Flat;
import com.realestate.emi.entity.Organization;
import com.realestate.emi.entity.Resident;
import com.realestate.emi.entity.ResidentPhone;
import com.realestate.emi.enums.ResidentPhoneLabel;
import com.realestate.emi.exception.ResourceNotFoundException;
import com.realestate.emi.exception.ServiceException;
import com.realestate.emi.mapper.ResidentMapper;
import com.realestate.emi.repository.FlatRepository;
import com.realestate.emi.repository.OrganizationRepository;
import com.realestate.emi.repository.ResidentPhoneRepository;
import com.realestate.emi.repository.ResidentRepository;
import com.realestate.emi.security.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ResidentService {

    private final ResidentRepository residentRepository;
    private final ResidentPhoneRepository residentPhoneRepository;
    private final FlatRepository flatRepository;
    private final OrganizationRepository organizationRepository;
    private final ResidentMapper residentMapper;
    private final TenantContext tenantContext;

    @Transactional(readOnly = true)
    public List<ResidentResponse> findAll(Long blockId, Long flatId, Boolean active) {
        Long orgId = tenantContext.getCurrentOrganizationId();
        List<Resident> residents = (flatId != null)
                ? residentRepository.findByOrganizationIdAndFlatIdOrderByFullNameAsc(orgId, flatId)
                : residentRepository.findByOrganizationIdOrderByFullNameAsc(orgId);
        return residents.stream()
                .filter(r -> blockId == null || (r.getFlat() != null && r.getFlat().getBlock() != null
                        && blockId.equals(r.getFlat().getBlock().getId())))
                .filter(r -> active == null || active.equals(r.getIsActive()))
                .map(this::toResponseWithPhones)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ResidentResponse findById(Long id) {
        Long orgId = tenantContext.getCurrentOrganizationId();
        Resident resident = residentRepository.findByIdAndOrganizationId(id, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Resident", id));
        return toResponseWithPhones(resident);
    }

    @Transactional
    public ResidentResponse create(ResidentRequest request) {
        Long orgId = tenantContext.getCurrentOrganizationId();
        Organization org = organizationRepository.findById(orgId)
                .orElseThrow(() -> new ServiceException("Organization not found", "ORG_NOT_FOUND"));
        Flat flat = flatRepository.findByIdAndOrganizationId(request.getFlatId(), orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Flat", request.getFlatId()));

        // ensure primary phone uniqueness within org (resident_phones table)
        if (residentPhoneRepository.existsByOrganizationIdAndPhone(orgId, request.getPrimaryPhone())) {
            throw new ServiceException("Phone '" + request.getPrimaryPhone() + "' is already registered to a resident in this society", "DUPLICATE_PHONE");
        }

        Resident resident = residentMapper.toEntity(request);
        resident.setOrganization(org);
        resident.setFlat(flat);
        resident.setIsActive(true);
        Resident saved = residentRepository.save(resident);

        // primary phone whitelist
        ResidentPhone primary = ResidentPhone.builder()
                .organization(org)
                .resident(saved)
                .phone(request.getPrimaryPhone())
                .label(ResidentPhoneLabel.PRIMARY)
                .isVerified(false)
                .build();
        residentPhoneRepository.save(primary);

        // additional phones
        if (request.getPhones() != null) {
            for (String phone : request.getPhones()) {
                if (!StringUtils.hasText(phone)) continue;
                if (phone.equals(request.getPrimaryPhone())) continue;
                if (residentPhoneRepository.existsByOrganizationIdAndPhone(orgId, phone)) {
                    throw new ServiceException("Phone '" + phone + "' is already registered to a resident in this society", "DUPLICATE_PHONE");
                }
                residentPhoneRepository.save(ResidentPhone.builder()
                        .organization(org)
                        .resident(saved)
                        .phone(phone)
                        .label(ResidentPhoneLabel.SECONDARY)
                        .isVerified(false)
                        .build());
            }
        }

        log.info("Created resident id={} for flat={}", saved.getId(), flat.getId());
        return toResponseWithPhones(saved);
    }

    @Transactional
    public ResidentResponse update(Long id, ResidentRequest request) {
        Long orgId = tenantContext.getCurrentOrganizationId();
        Resident existing = residentRepository.findByIdAndOrganizationId(id, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Resident", id));

        Flat flat = flatRepository.findByIdAndOrganizationId(request.getFlatId(), orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Flat", request.getFlatId()));

        residentMapper.updateEntityFromRequest(request, existing);
        existing.setFlat(flat);
        Resident saved = residentRepository.save(existing);
        return toResponseWithPhones(saved);
    }

    @Transactional
    public ResidentPhoneResponse addPhone(Long residentId, ResidentPhoneRequest request) {
        Long orgId = tenantContext.getCurrentOrganizationId();
        Resident resident = residentRepository.findByIdAndOrganizationId(residentId, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Resident", residentId));

        if (residentPhoneRepository.existsByOrganizationIdAndPhone(orgId, request.getPhone())) {
            throw new ServiceException("Phone '" + request.getPhone() + "' is already registered to a resident in this society", "DUPLICATE_PHONE");
        }

        ResidentPhone phone = ResidentPhone.builder()
                .organization(resident.getOrganization())
                .resident(resident)
                .phone(request.getPhone())
                .label(request.getLabel())
                .isVerified(false)
                .build();
        ResidentPhone saved = residentPhoneRepository.save(phone);
        return residentMapper.toPhoneResponse(saved);
    }

    @Transactional
    public void removePhone(Long residentId, Long phoneId) {
        Long orgId = tenantContext.getCurrentOrganizationId();
        Resident resident = residentRepository.findByIdAndOrganizationId(residentId, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Resident", residentId));

        ResidentPhone phone = residentPhoneRepository.findByIdAndResidentId(phoneId, resident.getId())
                .orElseThrow(() -> new ResourceNotFoundException("ResidentPhone", phoneId));

        if (phone.getLabel() == ResidentPhoneLabel.PRIMARY) {
            throw new ServiceException("Cannot remove the primary phone. Update the resident's primary phone instead.", "PRIMARY_PHONE_PROTECTED");
        }
        residentPhoneRepository.delete(phone);
    }

    @Transactional
    public void deactivate(Long id) {
        Long orgId = tenantContext.getCurrentOrganizationId();
        Resident existing = residentRepository.findByIdAndOrganizationId(id, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Resident", id));
        existing.setIsActive(false);
        residentRepository.save(existing);
    }

    private ResidentResponse toResponseWithPhones(Resident resident) {
        ResidentResponse resp = residentMapper.toResponse(resident);
        List<ResidentPhone> phones = residentPhoneRepository.findByResidentIdOrderByLabelAsc(resident.getId());
        List<ResidentPhoneResponse> phoneResponses = new ArrayList<>();
        for (ResidentPhone p : phones) {
            phoneResponses.add(residentMapper.toPhoneResponse(p));
        }
        resp.setPhones(phoneResponses);
        return resp;
    }
}
