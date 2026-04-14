package com.realestate.emi.service;

import com.realestate.emi.dto.response.NotificationResponse;
import com.realestate.emi.entity.InstallmentPhase;
import com.realestate.emi.entity.Notification;
import com.realestate.emi.entity.Organization;
import com.realestate.emi.enums.PhaseStatus;
import com.realestate.emi.repository.InstallmentPhaseRepository;
import com.realestate.emi.repository.NotificationRepository;
import com.realestate.emi.repository.OrganizationRepository;
import com.realestate.emi.security.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final InstallmentPhaseRepository installmentPhaseRepository;
    private final OrganizationRepository organizationRepository;
    private final TenantContext tenantContext;

    /**
     * Get notifications for the current user's org.
     */
    @Transactional(readOnly = true)
    public NotificationResponse getNotifications() {
        Long orgId = tenantContext.getCurrentOrganizationId();
        List<Notification> all = notificationRepository.findByOrganizationIdOrderByCreatedAtDesc(orgId);
        long unreadCount = notificationRepository.countByOrganizationIdAndIsReadFalse(orgId);
        long urgentCount = notificationRepository.countByOrganizationIdAndIsReadFalseAndSeverity(orgId, "critical");

        List<NotificationResponse.NotificationItem> items = all.stream()
                .limit(50)  // max 50 notifications
                .map(n -> NotificationResponse.NotificationItem.builder()
                        .id(n.getId())
                        .type(n.getType())
                        .severity(n.getSeverity())
                        .title(n.getTitle())
                        .message(n.getMessage())
                        .dealId(n.getDeal() != null ? n.getDeal().getId() : null)
                        .phaseId(n.getInstallmentPhase() != null ? n.getInstallmentPhase().getId() : null)
                        .customerName(n.getDeal() != null && n.getDeal().getCustomer() != null ? n.getDeal().getCustomer().getFullName() : null)
                        .phaseName(n.getInstallmentPhase() != null ? n.getInstallmentPhase().getPhaseName() : null)
                        .amount(n.getAmount())
                        .deadline(n.getDeadline())
                        .isRead(n.getIsRead())
                        .daysOverdue(n.getDeadline() != null && LocalDate.now().isAfter(n.getDeadline())
                                ? ChronoUnit.DAYS.between(n.getDeadline(), LocalDate.now()) : 0L)
                        .build())
                .collect(Collectors.toList());

        return NotificationResponse.builder()
                .totalCount((int) unreadCount)
                .urgentCount((int) urgentCount)
                .items(items)
                .build();
    }

    /**
     * Mark a single notification as read.
     */
    @Transactional
    public void markAsRead(Long notificationId) {
        Long orgId = tenantContext.getCurrentOrganizationId();
        notificationRepository.markAsRead(notificationId, orgId);
    }

    /**
     * Mark all notifications as read for current org.
     */
    @Transactional
    public void markAllAsRead() {
        Long orgId = tenantContext.getCurrentOrganizationId();
        notificationRepository.markAllAsRead(orgId);
    }

    /**
     * Called by scheduler — generates notifications for all orgs.
     * Creates notifications for phases that are overdue, due today, due tomorrow, due in 3 days.
     * Skips if notification already exists (using notificationKey for deduplication).
     */
    @Transactional
    public void generateNotifications() {
        LocalDate today = LocalDate.now();
        List<Organization> orgs = organizationRepository.findAll();

        for (Organization org : orgs) {
            if (!org.getIsActive()) continue;

            List<InstallmentPhase> phases = installmentPhaseRepository
                    .findUpcomingDuePhasesByOrg(today.plusDays(3), org.getId());

            for (InstallmentPhase phase : phases) {
                if (phase.getDueDeadline() == null) continue;

                long daysUntil = ChronoUnit.DAYS.between(today, phase.getDueDeadline());
                String type;
                String severity;
                String title;
                String message;

                if (daysUntil < 0) {
                    type = "OVERDUE";
                    severity = "critical";
                    title = "Payment Overdue — " + phase.getPhaseName();
                    message = phase.getDeal().getCustomer().getFullName() + " | " + Math.abs(daysUntil) + " days overdue | Interest accruing at 10% p.a.";
                } else if (daysUntil == 0) {
                    type = "DUE_TODAY";
                    severity = "critical";
                    title = "Payment Due Today — " + phase.getPhaseName();
                    message = phase.getDeal().getCustomer().getFullName() + " | Deadline today | Interest starts tomorrow if not paid";
                } else if (daysUntil == 1) {
                    type = "DUE_TOMORROW";
                    severity = "warning";
                    title = "Payment Due Tomorrow — " + phase.getPhaseName();
                    message = phase.getDeal().getCustomer().getFullName() + " | 1 day remaining";
                } else {
                    type = "DUE_SOON";
                    severity = "info";
                    title = "Payment Due in " + daysUntil + " Days — " + phase.getPhaseName();
                    message = phase.getDeal().getCustomer().getFullName() + " | " + daysUntil + " days remaining";
                }

                // Unique key: type changes daily for overdue (so new notification each day)
                String key = "DEAL-" + phase.getDeal().getId() + "-PHASE-" + phase.getId() + "-" + type + "-" + today;

                if (notificationRepository.existsByNotificationKey(key)) continue;

                Notification notification = Notification.builder()
                        .organization(org)
                        .deal(phase.getDeal())
                        .installmentPhase(phase)
                        .type(type)
                        .severity(severity)
                        .title(title)
                        .message(message)
                        .amount(phase.getDueAmount().subtract(phase.getPaidAmount()))
                        .deadline(phase.getDueDeadline())
                        .isRead(false)
                        .notificationKey(key)
                        .build();

                notificationRepository.save(notification);
                log.info("Notification created: [{}] {} for org {}", type, title, org.getCode());
            }
        }
    }
}
