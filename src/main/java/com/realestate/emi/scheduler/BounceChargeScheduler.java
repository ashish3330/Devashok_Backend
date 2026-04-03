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
            BigDecimal outstanding = schedule.getDueAmount().subtract(schedule.getPaidAmount());
            long daysOverdue = java.time.temporal.ChronoUnit.DAYS.between(schedule.getDueDate(), today);
            BigDecimal bounceCharge = outstanding
                    .multiply(BOUNCE_PENALTY_RATE)
                    .multiply(BigDecimal.valueOf(daysOverdue))
                    .divide(BigDecimal.valueOf(365), 2, RoundingMode.HALF_UP);

            schedule.setBounced(true);
            schedule.setBounceCharges(bounceCharge);

            log.info("Applied bounce charge {} on EMI schedule id={} (dealId={}, dueDate={}, outstanding={}, daysOverdue={})",
                    bounceCharge, schedule.getId(), schedule.getDeal().getId(),
                    schedule.getDueDate(), outstanding, daysOverdue);
        }

        emiScheduleRepository.saveAll(overdueEmis);
        log.info("Bounce charge scheduler completed. Processed {} EMI(s).", overdueEmis.size());
    }
}
