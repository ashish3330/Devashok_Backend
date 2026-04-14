package com.realestate.emi.scheduler;

import com.realestate.emi.service.InstallmentPhaseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OverdueInterestScheduler {

    private final InstallmentPhaseService installmentPhaseService;

    /**
     * Runs daily at midnight.
     * Finds construction phases where payment deadline (15 days) has passed
     * and applies interest on the outstanding amount.
     */
    @Scheduled(cron = "${scheduler.overdue-interest.cron:0 0 0 * * *}")
    public void applyOverdueInterest() {
        log.info("Running overdue interest scheduler...");
        installmentPhaseService.applyOverdueInterest();
    }
}
