package com.realestate.emi.service;

import com.realestate.emi.dto.request.FamilyMemberRequest;
import com.realestate.emi.dto.response.FamilyMemberResponse;
import com.realestate.emi.entity.FamilyMember;
import com.realestate.emi.entity.Organization;
import com.realestate.emi.entity.Resident;
import com.realestate.emi.exception.ResourceNotFoundException;
import com.realestate.emi.exception.ServiceException;
import com.realestate.emi.mapper.FamilyMemberMapper;
import com.realestate.emi.repository.FamilyMemberRepository;
import com.realestate.emi.repository.OrganizationRepository;
import com.realestate.emi.repository.ResidentRepository;
import com.realestate.emi.security.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class FamilyMemberService {

    private final FamilyMemberRepository familyMemberRepository;
    private final FamilyMemberMapper familyMemberMapper;
    private final TenantContext tenantContext;
    private final OrganizationRepository organizationRepository;
    private final ResidentRepository residentRepository;

    @Transactional(readOnly = true)
    public List<FamilyMemberResponse> listForResident() {
        Long orgId = requireOrgId();
        Long residentId = requireResidentId();
        return familyMemberRepository
                .findAllByResidentIdAndOrganizationId(residentId, orgId)
                .stream()
                .filter(m -> Boolean.TRUE.equals(m.getIsActive()))
                .map(familyMemberMapper::toResponse)
                .toList();
    }

    @Transactional
    public FamilyMemberResponse create(FamilyMemberRequest request) {
        Long orgId = requireOrgId();
        Long residentId = requireResidentId();

        Organization org = organizationRepository.findById(orgId)
                .orElseThrow(() -> new ServiceException("Organization not found", "ORG_NOT_FOUND"));
        Resident resident = residentRepository.findByIdAndOrganizationId(residentId, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Resident", residentId));
        if (resident.getFlat() == null) {
            throw new ServiceException("Resident has no flat assigned", "RESIDENT_FLAT_MISSING");
        }

        FamilyMember member = familyMemberMapper.toEntity(request);
        member.setOrganization(org);
        member.setResident(resident);
        member.setFlat(resident.getFlat());
        member.setIsActive(true);

        FamilyMember saved = familyMemberRepository.save(member);
        log.info("Resident {} added family member id={} relation={}", residentId, saved.getId(), saved.getRelation());
        return familyMemberMapper.toResponse(saved);
    }

    @Transactional
    public FamilyMemberResponse update(Long id, FamilyMemberRequest request) {
        Long orgId = requireOrgId();
        Long residentId = requireResidentId();

        FamilyMember member = familyMemberRepository.findByIdAndOrganizationId(id, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("FamilyMember", id));
        if (member.getResident() == null || !residentId.equals(member.getResident().getId())) {
            throw new ServiceException("Family member does not belong to this resident", "FAMILY_NOT_OWNED");
        }

        member.setFullName(request.getFullName());
        member.setRelation(request.getRelation());
        member.setAge(request.getAge());
        member.setPhone(request.getPhone());
        member.setEmail(request.getEmail());

        return familyMemberMapper.toResponse(familyMemberRepository.save(member));
    }

    @Transactional
    public void delete(Long id) {
        Long orgId = requireOrgId();
        Long residentId = requireResidentId();

        FamilyMember member = familyMemberRepository.findByIdAndOrganizationId(id, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("FamilyMember", id));
        if (member.getResident() == null || !residentId.equals(member.getResident().getId())) {
            throw new ServiceException("Family member does not belong to this resident", "FAMILY_NOT_OWNED");
        }
        member.setIsActive(false);
        familyMemberRepository.save(member);
        log.info("Resident {} deactivated family member id={}", residentId, id);
    }

    private Long requireOrgId() {
        Long orgId = tenantContext.getCurrentOrganizationId();
        if (orgId == null) {
            throw new ServiceException("Organization context missing", "ORG_CONTEXT_MISSING");
        }
        return orgId;
    }

    private Long requireResidentId() {
        Long residentId = tenantContext.getCurrentResidentId();
        if (residentId == null) {
            throw new ServiceException("Resident context missing", "RESIDENT_CONTEXT_MISSING");
        }
        return residentId;
    }
}
