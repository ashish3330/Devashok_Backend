package com.realestate.emi.service;

import com.realestate.emi.dto.response.ResidentNotificationResponse;
import com.realestate.emi.entity.Organization;
import com.realestate.emi.entity.Resident;
import com.realestate.emi.entity.ResidentNotification;
import com.realestate.emi.enums.NotificationCategory;
import com.realestate.emi.exception.ResourceNotFoundException;
import com.realestate.emi.exception.ServiceException;
import com.realestate.emi.mapper.ResidentNotificationMapper;
import com.realestate.emi.repository.ResidentNotificationRepository;
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
public class ResidentNotificationService {

    private final ResidentNotificationRepository residentNotificationRepository;
    private final ResidentNotificationMapper residentNotificationMapper;
    private final TenantContext tenantContext;

    @Transactional(readOnly = true)
    public List<ResidentNotificationResponse> feed() {
        Long orgId = tenantContext.getCurrentOrganizationId();
        Long residentId = tenantContext.getCurrentResidentId();
        requireResidentContext(orgId, residentId);
        return residentNotificationRepository
                .findTop50ByResidentIdAndOrganizationIdOrderByCreatedAtDesc(residentId, orgId)
                .stream()
                .map(residentNotificationMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public long unreadCount() {
        Long orgId = tenantContext.getCurrentOrganizationId();
        Long residentId = tenantContext.getCurrentResidentId();
        requireResidentContext(orgId, residentId);
        return residentNotificationRepository.countByResidentIdAndOrganizationIdAndReadAtIsNull(residentId, orgId);
    }

    @Transactional
    public ResidentNotificationResponse markRead(Long id) {
        Long orgId = tenantContext.getCurrentOrganizationId();
        Long residentId = tenantContext.getCurrentResidentId();
        requireResidentContext(orgId, residentId);
        ResidentNotification notification = residentNotificationRepository
                .findByIdAndResidentIdAndOrganizationId(id, residentId, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("ResidentNotification", id));
        if (notification.getReadAt() == null) {
            notification.setReadAt(LocalDateTime.now());
            notification = residentNotificationRepository.save(notification);
        }
        return residentNotificationMapper.toResponse(notification);
    }

    @Transactional
    public int markAllRead() {
        Long orgId = tenantContext.getCurrentOrganizationId();
        Long residentId = tenantContext.getCurrentResidentId();
        requireResidentContext(orgId, residentId);
        return residentNotificationRepository.markAllRead(residentId, orgId, LocalDateTime.now());
    }

    /**
     * Public hook for other services to enqueue a notification for a resident.
     * Caller is responsible for supplying a managed Resident; the resident's
     * organization is reused so org-isolation is preserved automatically.
     */
    @Transactional
    public ResidentNotification enqueue(Resident resident,
                                         NotificationCategory category,
                                         String title,
                                         String body,
                                         String referenceType,
                                         Long referenceId) {
        if (resident == null) {
            throw new ServiceException("Resident is required to enqueue notification", "INVALID_NOTIFICATION_TARGET");
        }
        Organization org = resident.getOrganization();
        if (org == null) {
            throw new ServiceException("Resident is not bound to an organization", "INVALID_NOTIFICATION_TARGET");
        }
        ResidentNotification notification = ResidentNotification.builder()
                .organization(org)
                .resident(resident)
                .title(title)
                .body(body)
                .category(category)
                .referenceType(referenceType)
                .referenceId(referenceId)
                .build();
        ResidentNotification saved = residentNotificationRepository.save(notification);
        log.debug("Enqueued notification id={} category={} resident={}", saved.getId(), category, resident.getId());
        return saved;
    }

    private void requireResidentContext(Long orgId, Long residentId) {
        if (orgId == null || residentId == null) {
            throw new ServiceException("Resident context missing", "RESIDENT_CONTEXT_MISSING");
        }
    }
}
