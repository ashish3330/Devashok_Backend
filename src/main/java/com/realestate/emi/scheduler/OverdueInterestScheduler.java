package com.realestate.emi.scheduler;

import com.realestate.emi.entity.InstallmentPhase;
import com.realestate.emi.enums.PhaseStatus;
import com.realestate.emi.repository.InstallmentPhaseRepository;
import com.realestate.emi.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class OverdueInterestScheduler {

    private static final BigDecimal INTEREST_RATE_ANNUAL = new BigDecimal("10.00"); // 10% p.a.

    private final InstallmentPhaseRepository installmentPhaseRepository;
    private final NotificationService notificationService;

    /**
     * Runs daily at 8:00 AM.
     *
     * 1. Finds all DUE/PARTIAL phases where deadline has passed → marks OVERDUE + applies interest
     * 2. Logs phases due today, tomorrow, and within 3 days for visibility
     */
    @Scheduled(cron = "${scheduler.overdue-interest.cron:0 0 8 * * *}")
    @Transactional
    public void dailyPhaseCheck() {
        LocalDate today = LocalDate.now();
        log.info("═══════════════════════════════════════════════════════");
        log.info("Daily Phase Check — {}", today);
        log.info("═══════════════════════════════════════════════════════");

        // 1. Mark overdue + apply interest on TOTAL DEAL OUTSTANDING (not just the phase)
        List<InstallmentPhase> overduePhases = installmentPhaseRepository.findOverduePhases(today);
        int interestApplied = 0;

        for (InstallmentPhase phase : overduePhases) {
            BigDecimal phaseOutstanding = phase.getDueAmount().subtract(phase.getPaidAmount());
            if (phaseOutstanding.compareTo(BigDecimal.ZERO) <= 0) continue;

            // Calculate TOTAL outstanding across ALL unpaid phases of the same deal
            List<InstallmentPhase> allDealPhases = installmentPhaseRepository.findByDealOrderByPhaseOrderAsc(phase.getDeal());
            BigDecimal totalDealOutstanding = allDealPhases.stream()
                    .filter(p -> p.getStatus() != PhaseStatus.PAID && p.getStatus() != PhaseStatus.PENDING)
                    .map(p -> p.getDueAmount().subtract(p.getPaidAmount()).max(BigDecimal.ZERO))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            if (totalDealOutstanding.compareTo(BigDecimal.ZERO) <= 0) continue;

            long daysOverdue = ChronoUnit.DAYS.between(phase.getDueDeadline(), today);

            // Interest = TOTAL DEAL outstanding × (10/100) × daysOverdue / 365, rounded to nearest rupee
            BigDecimal interest = totalDealOutstanding
                    .multiply(INTEREST_RATE_ANNUAL)
                    .multiply(BigDecimal.valueOf(daysOverdue))
                    .divide(BigDecimal.valueOf(36500), 0, RoundingMode.HALF_UP);

            phase.setInterestAmount(interest);
            phase.setStatus(PhaseStatus.OVERDUE);
            installmentPhaseRepository.save(phase);
            interestApplied++;

            log.warn("⚠ OVERDUE: Deal #{} | {} | Customer: {} | Phase Outstanding: ₹{} | Total Deal Outstanding: ₹{} | {} days overdue | Interest: ₹{}",
                    phase.getDeal().getId(),
                    phase.getPhaseName(),
                    phase.getDeal().getCustomer().getFullName(),
                    phaseOutstanding.setScale(0, RoundingMode.HALF_UP),
                    totalDealOutstanding.setScale(0, RoundingMode.HALF_UP),
                    daysOverdue,
                    interest);
        }

        if (interestApplied > 0) {
            log.info("Applied interest to {} overdue phase(s)", interestApplied);
        }

        // 2. Log phases due today
        List<InstallmentPhase> allDuePhases = installmentPhaseRepository
                .findAll().stream()
                .filter(p -> p.getDueDeadline() != null && p.getStatus() != PhaseStatus.PAID && p.getStatus() != PhaseStatus.PENDING)
                .toList();

        long dueToday = allDuePhases.stream()
                .filter(p -> p.getDueDeadline().equals(today))
                .peek(p -> log.warn("🔴 DUE TODAY: Deal #{} | {} | Customer: {} | ₹{}",
                        p.getDeal().getId(), p.getPhaseName(),
                        p.getDeal().getCustomer().getFullName(),
                        p.getDueAmount().setScale(0, RoundingMode.HALF_UP)))
                .count();

        // 3. Log phases due tomorrow
        long dueTomorrow = allDuePhases.stream()
                .filter(p -> p.getDueDeadline().equals(today.plusDays(1)))
                .peek(p -> log.info("🟡 DUE TOMORROW: Deal #{} | {} | Customer: {} | ₹{}",
                        p.getDeal().getId(), p.getPhaseName(),
                        p.getDeal().getCustomer().getFullName(),
                        p.getDueAmount().setScale(0, RoundingMode.HALF_UP)))
                .count();

        // 4. Log phases due within 3 days
        long dueSoon = allDuePhases.stream()
                .filter(p -> {
                    long daysUntil = ChronoUnit.DAYS.between(today, p.getDueDeadline());
                    return daysUntil >= 2 && daysUntil <= 3;
                })
                .peek(p -> log.info("🔵 DUE IN {} DAYS: Deal #{} | {} | Customer: {} | ₹{}",
                        ChronoUnit.DAYS.between(today, p.getDueDeadline()),
                        p.getDeal().getId(), p.getPhaseName(),
                        p.getDeal().getCustomer().getFullName(),
                        p.getDueAmount().setScale(0, RoundingMode.HALF_UP)))
                .count();

        log.info("───────────────────────────────────────────────────────");
        log.info("Summary: {} overdue (interest applied), {} due today, {} due tomorrow, {} due in 2-3 days",
                interestApplied, dueToday, dueTomorrow, dueSoon);
        log.info("═══════════════════════════════════════════════════════");

        // Generate database-backed notifications
        log.info("Generating notifications...");
        notificationService.generateNotifications();
    }
}
