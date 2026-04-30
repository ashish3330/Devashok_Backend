package com.realestate.emi.scheduler;

import com.realestate.emi.entity.EmiSchedule;
import com.realestate.emi.enums.EmiStatus;
import com.realestate.emi.repository.EmiScheduleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class BounceChargeScheduler {

    private static final BigDecimal BOUNCE_PENALTY_RATE = new BigDecimal("0.10");

    private final EmiScheduleRepository emiScheduleRepository;

    /**
     * Runs every day at midnight.
     * Finds all overdue EMIs (due date passed, not yet paid, not already bounced)
     * and applies a 10% bounce charge on the outstanding amount.
     */
    @Scheduled(cron = "${scheduler.bounce.cron:0 0 0 * * *}")
    @Transactional
    public void applyBounceCharges() {
        LocalDate today = LocalDate.now();
        log.info("Running bounce charge scheduler for date: {}", today);

        List<EmiSchedule> overdueEmis = emiScheduleRepository.findOverdueUnbouncedEmis(today);

        if (overdueEmis.isEmpty()) {
            log.info("No overdue EMIs found for bounce processing.");
            return;
        }

        log.info("Found {} overdue EMI(s) to apply bounce charges.", overdueEmis.size());

        for (EmiSchedule schedule : overdueEmis) {
            BigDecimal emiOutstanding = schedule.getDueAmount().subtract(schedule.getPaidAmount());

            // Calculate TOTAL outstanding across ALL unpaid EMIs of the same deal
            List<EmiSchedule> allDealEmis = emiScheduleRepository.findByDealOrderByDueDateAsc(schedule.getDeal());
            BigDecimal totalDealOutstanding = allDealEmis.stream()
                    .filter(e -> e.getStatus() != EmiStatus.PAID)
                    .map(e -> e.getDueAmount().subtract(e.getPaidAmount()).max(BigDecimal.ZERO))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            if (totalDealOutstanding.compareTo(BigDecimal.ZERO) <= 0) continue;

            long daysOverdue = java.time.temporal.ChronoUnit.DAYS.between(schedule.getDueDate(), today);

            // Bounce charge = TOTAL DEAL outstanding × 10% × daysOverdue / 365
            BigDecimal bounceCharge = totalDealOutstanding
                    .multiply(BOUNCE_PENALTY_RATE)
                    .multiply(BigDecimal.valueOf(daysOverdue))
                    .divide(BigDecimal.valueOf(365), 2, RoundingMode.HALF_UP);

            schedule.setBounced(true);
            schedule.setBounceCharges(bounceCharge);

            log.info("Applied bounce charge ₹{} on EMI id={} (dealId={}, dueDate={}, EMI outstanding=₹{}, TOTAL deal outstanding=₹{}, {} days overdue)",
                    bounceCharge, schedule.getId(), schedule.getDeal().getId(),
                    schedule.getDueDate(), emiOutstanding, totalDealOutstanding, daysOverdue);
        }

        emiScheduleRepository.saveAll(overdueEmis);
        log.info("Bounce charge scheduler completed. Processed {} EMI(s).", overdueEmis.size());
    }
}
