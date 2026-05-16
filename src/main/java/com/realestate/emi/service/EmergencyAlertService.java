package com.realestate.emi.service;

import com.realestate.emi.dto.request.EmergencyAlertRequest;
import com.realestate.emi.dto.response.EmergencyAlertResponse;
import com.realestate.emi.entity.EmergencyAlert;
import com.realestate.emi.entity.Flat;
import com.realestate.emi.entity.Organization;
import com.realestate.emi.entity.Resident;
import com.realestate.emi.entity.User;
import com.realestate.emi.enums.EmergencyStatus;
import com.realestate.emi.exception.ResourceNotFoundException;
import com.realestate.emi.exception.ServiceException;
import com.realestate.emi.mapper.EmergencyAlertMapper;
import com.realestate.emi.repository.EmergencyAlertRepository;
import com.realestate.emi.repository.FlatRepository;
import com.realestate.emi.repository.OrganizationRepository;
import com.realestate.emi.repository.ResidentRepository;
import com.realestate.emi.repository.UserRepository;
import com.realestate.emi.security.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmergencyAlertService {

    private final EmergencyAlertRepository emergencyAlertRepository;
    private final EmergencyAlertMapper emergencyAlertMapper;
    private final TenantContext tenantContext;
    private final OrganizationRepository organizationRepository;
    private final ResidentRepository residentRepository;
    private final FlatRepository flatRepository;
    private final UserRepository userRepository;

    @Transactional
    public EmergencyAlertResponse raise(EmergencyAlertRequest request) {
        Long orgId = tenantContext.getCurrentOrganizationId();
        Long residentId = tenantContext.getCurrentResidentId();
        Long flatId = tenantContext.getCurrentFlatId();
        if (orgId == null || residentId == null || flatId == null) {
            throw new ServiceException("Resident context missing", "RESIDENT_CONTEXT_MISSING");
        }

        Organization org = organizationRepository.findById(orgId)
                .orElseThrow(() -> new ServiceException("Organization not found", "ORG_NOT_FOUND"));
        Resident resident = residentRepository.findByIdAndOrganizationId(residentId, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Resident", residentId));
        Flat flat = flatRepository.findByIdAndOrganizationId(flatId, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Flat", flatId));

        EmergencyAlert alert = EmergencyAlert.builder()
                .organization(org)
                .resident(resident)
                .flat(flat)
                .type(request.getType())
                .message(request.getMessage())
                .location(request.getLocation())
                .status(EmergencyStatus.ACTIVE)
                .build();

        EmergencyAlert saved = emergencyAlertRepository.save(alert);
        log.warn("Emergency SOS raised id={} type={} resident={} flat={} org={}",
                saved.getId(), saved.getType(), residentId, flatId, orgId);
        return emergencyAlertMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<EmergencyAlertResponse> listMine() {
        Long orgId = tenantContext.getCurrentOrganizationId();
        Long residentId = tenantContext.getCurrentResidentId();
        if (orgId == null || residentId == null) {
            throw new ServiceException("Resident context missing", "RESIDENT_CONTEXT_MISSING");
        }
        return emergencyAlertRepository
                .findAllByResidentIdAndOrganizationIdOrderByCreatedAtDesc(residentId, orgId)
                .stream()
                .map(emergencyAlertMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<EmergencyAlertResponse> listForAdmin(EmergencyStatus statusFilter) {
        Long orgId = tenantContext.getCurrentOrganizationId();
        List<EmergencyAlert> alerts = statusFilter == null
                ? emergencyAlertRepository.findAllByOrganizationIdOrderByCreatedAtDesc(orgId)
                : emergencyAlertRepository.findAllByOrganizationIdAndStatusOrderByCreatedAtDesc(orgId, statusFilter);
        return alerts.stream().map(emergencyAlertMapper::toResponse).collect(Collectors.toList());
    }

    @Transactional
    public EmergencyAlertResponse acknowledge(Long id) {
        Long orgId = tenantContext.getCurrentOrganizationId();
        Long userId = tenantContext.getCurrentUserId();
        EmergencyAlert alert = emergencyAlertRepository.findByIdAndOrganizationId(id, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("EmergencyAlert", id));
        if (alert.getStatus() == EmergencyStatus.RESOLVED) {
            throw new ServiceException("Cannot acknowledge a resolved alert", "INVALID_EMERGENCY_STATE");
        }
        if (userId != null) {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("User", userId));
            alert.setAcknowledgedBy(user);
        }
        alert.setStatus(EmergencyStatus.ACKNOWLEDGED);
        alert.setAcknowledgedAt(LocalDateTime.now());
        EmergencyAlert saved = emergencyAlertRepository.save(alert);
        log.info("Emergency alert id={} acknowledged by userId={}", saved.getId(), userId);
        return emergencyAlertMapper.toResponse(saved);
    }

    @Transactional
    public EmergencyAlertResponse resolve(Long id) {
        Long orgId = tenantContext.getCurrentOrganizationId();
        Long userId = tenantContext.getCurrentUserId();
        EmergencyAlert alert = emergencyAlertRepository.findByIdAndOrganizationId(id, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("EmergencyAlert", id));
        if (alert.getStatus() == EmergencyStatus.RESOLVED) {
            throw new ServiceException("Alert is already resolved", "INVALID_EMERGENCY_STATE");
        }
        if (alert.getAcknowledgedBy() == null && userId != null) {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("User", userId));
            alert.setAcknowledgedBy(user);
            if (alert.getAcknowledgedAt() == null) {
                alert.setAcknowledgedAt(LocalDateTime.now());
            }
        }
        alert.setStatus(EmergencyStatus.RESOLVED);
        alert.setResolvedAt(LocalDateTime.now());
        EmergencyAlert saved = emergencyAlertRepository.save(alert);
        log.info("Emergency alert id={} resolved by userId={}", saved.getId(), userId);
        return emergencyAlertMapper.toResponse(saved);
    }
}
