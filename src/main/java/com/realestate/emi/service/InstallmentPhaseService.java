package com.realestate.emi.service;

import com.realestate.emi.dto.request.PaymentRequest;
import com.realestate.emi.dto.response.InstallmentPhaseResponse;
import com.realestate.emi.dto.response.PaymentResponse;
import com.realestate.emi.entity.Deal;
import com.realestate.emi.entity.InstallmentPhase;
import com.realestate.emi.entity.Payment;
import com.realestate.emi.enums.DealStatus;
import com.realestate.emi.enums.PaymentMethod;
import com.realestate.emi.enums.PhaseStatus;
import com.realestate.emi.enums.PlanType;
import com.realestate.emi.exception.ResourceNotFoundException;
import com.realestate.emi.exception.ServiceException;
import com.realestate.emi.mapper.InstallmentPhaseMapper;
import com.realestate.emi.mapper.PaymentMapper;
import com.realestate.emi.repository.DealRepository;
import com.realestate.emi.repository.InstallmentPhaseRepository;
import com.realestate.emi.repository.PaymentRepository;
import com.realestate.emi.security.CustomPrincipal;
import com.realestate.emi.security.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class InstallmentPhaseService {

    private static final int PAYMENT_DEADLINE_DAYS = 15;
    private static final BigDecimal OVERDUE_INTEREST_RATE_ANNUAL = new BigDecimal("10.00"); // 10% p.a.

    private final InstallmentPhaseRepository installmentPhaseRepository;
    private final DealRepository dealRepository;
    private final PaymentRepository paymentRepository;
    private final InstallmentPhaseMapper installmentPhaseMapper;
    private final PaymentMapper paymentMapper;
    private final TenantContext tenantContext;

    @Transactional(readOnly = true)
    public List<InstallmentPhaseResponse> getPhasesByDeal(Long dealId) {
        Deal deal = dealRepository.findById(dealId)
                .orElseThrow(() -> new ResourceNotFoundException("Deal", dealId));
        Long orgId = tenantContext.getCurrentOrganizationId();
        if (deal.getOrganization() != null && !deal.getOrganization().getId().equals(orgId)) {
            throw new ResourceNotFoundException("Deal", dealId);
        }

        // Recalculate interest on the fly for display
        List<InstallmentPhase> phases = installmentPhaseRepository.findByDealOrderByPhaseOrderAsc(deal);
        phases.forEach(this::recalculateInterestIfOverdue);

        return installmentPhaseMapper.toResponseList(phases);
    }

    /**
     * Admin activates a construction phase — makes it DUE for payment with 15-day deadline.
     * Only PENDING phases can be activated. Previous phase must be PAID first.
     */
    @Transactional
    public InstallmentPhaseResponse activatePhase(Long dealId, Long phaseId) {
        Deal deal = validateConstructionDeal(dealId);

        InstallmentPhase phase = installmentPhaseRepository.findByIdAndDeal(phaseId, deal)
                .orElseThrow(() -> new ResourceNotFoundException("InstallmentPhase", phaseId));

        if (phase.getStatus() == PhaseStatus.PAID) {
            throw new ServiceException("Phase is already paid", "PHASE_ALREADY_PAID");
        }

        if (phase.getStatus() == PhaseStatus.DUE || phase.getStatus() == PhaseStatus.OVERDUE) {
            throw new ServiceException("Phase is already active and awaiting payment", "PHASE_ALREADY_ACTIVE");
        }

        if (phase.getStatus() != PhaseStatus.PENDING) {
            throw new ServiceException("Phase is in status " + phase.getStatus() + " and cannot be activated", "INVALID_PHASE_STATUS");
        }

        // Validate previous phase is paid (except phase 1)
        if (phase.getPhaseOrder() > 1) {
            List<InstallmentPhase> allPhases = installmentPhaseRepository.findByDealOrderByPhaseOrderAsc(deal);
            InstallmentPhase previousPhase = allPhases.stream()
                    .filter(p -> p.getPhaseOrder() == phase.getPhaseOrder() - 1)
                    .findFirst().orElse(null);
            if (previousPhase != null && previousPhase.getStatus() != PhaseStatus.PAID) {
                throw new ServiceException("Previous phase '" + previousPhase.getPhaseName() + "' must be paid before activating this phase", "PREVIOUS_PHASE_NOT_PAID");
            }
        }

        // Activate phase — DUE with 15-day deadline
        phase.setStatus(PhaseStatus.DUE);
        phase.setMarkedDueAt(LocalDateTime.now());
        phase.setDueDeadline(LocalDate.now().plusDays(PAYMENT_DEADLINE_DAYS));
        installmentPhaseRepository.save(phase);

        // Update deal next due date
        deal.setNextDueDate(phase.getDueDeadline());
        dealRepository.save(deal);

        log.info("Phase '{}' activated for deal {}. Payment due by {}",
                phase.getPhaseName(), dealId, phase.getDueDeadline());

        return installmentPhaseMapper.toResponse(phase);
    }

    // Backward compat alias
    @Transactional
    public InstallmentPhaseResponse markPhaseConstructionComplete(Long dealId, Long phaseId) {
        return activatePhase(dealId, phaseId);
    }

    /**
     * Record payment against a specific phase.
     * Payment is only allowed for DUE, OVERDUE, or PARTIAL phases.
     * Interest is included in total due if payment is overdue.
     */
    @Transactional
    public PaymentResponse payPhase(Long dealId, Long phaseId, PaymentRequest request) {
        Deal deal = validateConstructionDeal(dealId);

        if (request.getPaymentMethod() == PaymentMethod.INITIAL_DEPOSIT) {
            throw new ServiceException("INITIAL_DEPOSIT is not allowed for manual payments", "INVALID_PAYMENT_METHOD");
        }

        InstallmentPhase phase = installmentPhaseRepository.findByIdAndDeal(phaseId, deal)
                .orElseThrow(() -> new ResourceNotFoundException("InstallmentPhase", phaseId));

        if (phase.getStatus() == PhaseStatus.PAID) {
            throw new ServiceException("This phase is already fully paid", "PHASE_ALREADY_PAID");
        }

        if (phase.getStatus() == PhaseStatus.PENDING) {
            throw new ServiceException("Construction for this phase has not been completed yet. Admin must mark construction complete first.", "PHASE_NOT_DUE");
        }

        // Recalculate interest before accepting payment
        recalculateInterestIfOverdue(phase);

        BigDecimal totalDue = phase.getDueAmount().add(phase.getInterestAmount());
        BigDecimal remainingDue = totalDue.subtract(phase.getPaidAmount());
        remainingDue = remainingDue.setScale(0, RoundingMode.HALF_UP);

        if (request.getAmount().compareTo(remainingDue) > 0) {
            throw new ServiceException("Payment amount ₹" + request.getAmount().setScale(0, RoundingMode.HALF_UP)
                    + " exceeds remaining due ₹" + remainingDue, "AMOUNT_EXCEEDS_DUE");
        }

        // Update phase payment
        phase.setPaidAmount(phase.getPaidAmount().add(request.getAmount()));
        // Mark PAID if paid amount covers the base due amount (interest forgiven on full payment)
        // OR if paid amount covers totalDue (base + interest)
        if (phase.getPaidAmount().compareTo(phase.getDueAmount()) >= 0) {
            phase.setStatus(PhaseStatus.PAID);
            // Clear interest if base amount is fully covered
            if (phase.getPaidAmount().compareTo(totalDue) < 0) {
                log.info("Phase {} base amount fully paid, waiving remaining interest for deal {}", phase.getPhaseName(), dealId);
            }
            log.info("Phase {} fully paid for deal {}", phase.getPhaseName(), dealId);
        } else {
            phase.setStatus(PhaseStatus.PARTIAL);
        }
        installmentPhaseRepository.save(phase);

        // Create payment record
        Payment payment = Payment.builder()
                .deal(deal)
                .installmentPhase(phase)
                .amount(request.getAmount())
                .paymentDate(request.getPaymentDate())
                .paymentMethod(request.getPaymentMethod())
                .utrNumber(request.getUtrNumber())
                .notes(request.getNotes())
                .createdByAdmin(getAdminUsername())
                .build();
        payment = paymentRepository.save(payment);

        log.info("Payment ₹{} recorded for phase {} of deal {}", request.getAmount(), phase.getPhaseName(), dealId);

        // Check deal completion
        checkConstructionDealCompletion(deal);

        return paymentMapper.toResponse(payment);
    }

    /**
     * Scheduled or on-demand: recalculate interest for overdue phases.
     * Interest = (outstanding * annualRate / 100 * daysOverdue) / 365, rounded to nearest rupee.
     */
    @Transactional
    public void applyOverdueInterest() {
        LocalDate today = LocalDate.now();
        List<InstallmentPhase> overduePhases = installmentPhaseRepository.findOverduePhases(today);

        for (InstallmentPhase phase : overduePhases) {
            recalculateInterestIfOverdue(phase);
            phase.setStatus(PhaseStatus.OVERDUE);
            installmentPhaseRepository.save(phase);
        }

        if (!overduePhases.isEmpty()) {
            log.info("Applied overdue interest to {} phases", overduePhases.size());
        }
    }

    private void recalculateInterestIfOverdue(InstallmentPhase phase) {
        if (phase.getDueDeadline() == null || phase.getStatus() == PhaseStatus.PAID || phase.getStatus() == PhaseStatus.PENDING) {
            return;
        }

        LocalDate today = LocalDate.now();
        if (today.isAfter(phase.getDueDeadline())) {
            long daysOverdue = ChronoUnit.DAYS.between(phase.getDueDeadline(), today);
            BigDecimal outstanding = phase.getDueAmount().subtract(phase.getPaidAmount());
            if (outstanding.compareTo(BigDecimal.ZERO) <= 0) return;

            // Interest = outstanding * (rate/100) * daysOverdue / 365
            BigDecimal interest = outstanding
                    .multiply(OVERDUE_INTEREST_RATE_ANNUAL)
                    .multiply(BigDecimal.valueOf(daysOverdue))
                    .divide(BigDecimal.valueOf(36500), 0, RoundingMode.HALF_UP);

            phase.setInterestAmount(interest);

            if (phase.getStatus() == PhaseStatus.DUE) {
                phase.setStatus(PhaseStatus.OVERDUE);
            }
        }
    }

    private Deal validateConstructionDeal(Long dealId) {
        Deal deal = dealRepository.findByIdWithDetails(dealId)
                .orElseThrow(() -> new ResourceNotFoundException("Deal", dealId));
        Long orgId = tenantContext.getCurrentOrganizationId();
        if (deal.getOrganization() != null && !deal.getOrganization().getId().equals(orgId)) {
            throw new ResourceNotFoundException("Deal", dealId);
        }

        if (deal.getPlanType() != PlanType.CONSTRUCTION_LINKED) {
            throw new ServiceException("This deal is not a construction-linked plan", "INVALID_PLAN_TYPE");
        }
        if (deal.getStatus() == DealStatus.COMPLETED) {
            throw new ServiceException("Deal is already completed", "DEAL_COMPLETED");
        }
        return deal;
    }

    private void checkConstructionDealCompletion(Deal deal) {
        List<InstallmentPhase> allPhases = installmentPhaseRepository.findByDealOrderByPhaseOrderAsc(deal);
        boolean allPaid = !allPhases.isEmpty()
                && allPhases.stream().allMatch(p -> p.getStatus() == PhaseStatus.PAID);
        if (allPaid) {
            deal.setStatus(DealStatus.COMPLETED);
            deal.setNextDueDate(null);
            dealRepository.save(deal);
            log.info("Construction-linked deal {} marked as COMPLETED - all phases paid", deal.getId());
        }
    }

    private String getAdminUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof CustomPrincipal principal) {
            return principal.getEmail();
        }
        if (authentication != null) {
            return authentication.getName();
        }
        return "unknown";
    }
}
