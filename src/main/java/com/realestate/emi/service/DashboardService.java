package com.realestate.emi.service;

import com.realestate.emi.dto.response.*;
import com.realestate.emi.entity.EmiSchedule;
import com.realestate.emi.enums.DealStatus;
import com.realestate.emi.enums.EmiStatus;
import com.realestate.emi.repository.CustomerRepository;
import com.realestate.emi.repository.DealRepository;
import com.realestate.emi.repository.EmiScheduleRepository;
import com.realestate.emi.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardService {

    private final DealRepository dealRepository;
    private final PaymentRepository paymentRepository;
    private final EmiScheduleRepository emiScheduleRepository;
    private final CustomerRepository customerRepository;

    // ── Legacy summary (kept for backward compat) ───────────────────────────

    @Transactional(readOnly = true)
    public DashboardResponse getSummary() {
        return DashboardResponse.builder()
                .totalDeals(dealRepository.count())
                .activeDeals(dealRepository.countByStatus(DealStatus.ACTIVE))
                .completedDeals(dealRepository.countByStatus(DealStatus.COMPLETED))
                .defaultedDeals(dealRepository.countByStatus(DealStatus.DEFAULTED))
                .totalDealAmount(dealRepository.sumTotalPayableAfterDeposit())
                .totalReceived(paymentRepository.sumAllPayments())
                .totalOutstanding(emiScheduleRepository.sumTotalOutstanding())
                .build();
    }

    // ── Monthly analytics ────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public MonthlyAnalyticsResponse getMonthlyAnalytics(int year, int month) {
        long completed = emiScheduleRepository.countByStatusAndMonth(EmiStatus.PAID, year, month);
        long pending   = emiScheduleRepository.countByStatusAndMonth(EmiStatus.PENDING, year, month);
        long partial   = emiScheduleRepository.countByStatusAndMonth(EmiStatus.PARTIAL, year, month);

        return MonthlyAnalyticsResponse.builder()
                .year(year).month(month)
                .paymentsCompleted(completed)
                .paymentsPending(pending)
                .paymentsPartial(partial)
                .amountReceived(paymentRepository.sumByMonth(year, month))
                .amountPending(emiScheduleRepository.sumOutstandingByMonth(year, month))
                .build();
    }

    // ── Comprehensive industry-level analytics ───────────────────────────────

    @Transactional(readOnly = true)
    public ComprehensiveAnalyticsResponse getComprehensiveAnalytics() {
        log.debug("Building comprehensive analytics");
        LocalDate today = LocalDate.now();

        return ComprehensiveAnalyticsResponse.builder()
                .portfolioOverview(buildPortfolioOverview(today))
                .upcomingThisWeek(buildUpcomingWindow(today, today.plusDays(7), "This Week"))
                .upcomingNextMonth(buildUpcomingWindow(today, today.plusDays(30), "Next 30 Days"))
                .overdueAnalysis(buildOverdueAnalysis(today))
                .monthlyTrend(buildMonthlyTrend(today))
                .dealDistribution(buildDealDistribution())
                .build();
    }

    // ── Portfolio overview ───────────────────────────────────────────────────

    private PortfolioOverviewResponse buildPortfolioOverview(LocalDate today) {
        long totalDeals     = dealRepository.count();
        long activeDeals    = dealRepository.countByStatus(DealStatus.ACTIVE);
        long completedDeals = dealRepository.countByStatus(DealStatus.COMPLETED);
        long defaultedDeals = dealRepository.countByStatus(DealStatus.DEFAULTED);

        BigDecimal totalCollected   = coalesce(paymentRepository.sumAllPayments());
        BigDecimal totalOutstanding = coalesce(emiScheduleRepository.sumTotalOutstanding());
        BigDecimal totalPortfolio   = coalesce(dealRepository.sumTotalPayableAfterDeposit());

        BigDecimal totalPayable = totalCollected.add(totalOutstanding);
        double efficiency = totalPayable.compareTo(BigDecimal.ZERO) == 0 ? 0.0
                : totalCollected.multiply(BigDecimal.valueOf(100))
                                .divide(totalPayable, 2, RoundingMode.HALF_UP)
                                .doubleValue();

        long npaDeals = emiScheduleRepository.countNpaDeals(today.minusDays(90));
        double npaRate = activeDeals == 0 ? 0.0
                : round2((double) npaDeals / activeDeals * 100);

        return PortfolioOverviewResponse.builder()
                .totalDeals(totalDeals)
                .activeDeals(activeDeals)
                .completedDeals(completedDeals)
                .defaultedDeals(defaultedDeals)
                .totalPortfolioValue(totalPortfolio)
                .totalCollected(totalCollected)
                .totalOutstanding(totalOutstanding)
                .collectionEfficiencyPercent(efficiency)
                .npaDeals(npaDeals)
                .npaRatePercent(npaRate)
                .averageDealValue(coalesce(dealRepository.avgDealValue()))
                .averageEmiAmount(coalesce(dealRepository.avgEmiAmount()))
                .totalCustomers(customerRepository.count())
                .build();
    }

    // ── Upcoming EMI windows ─────────────────────────────────────────────────

    private UpcomingEmiWindow buildUpcomingWindow(LocalDate from, LocalDate to, String label) {
        List<EmiSchedule> schedules = emiScheduleRepository.findUpcomingEmis(from, to);

        List<UpcomingEmiItem> items = schedules.stream().map(e -> UpcomingEmiItem.builder()
                .emiId(e.getId())
                .dealId(e.getDeal().getId())
                .customerName(e.getDeal().getCustomer().getFullName())
                .customerPhone(e.getDeal().getCustomer().getPhoneNumber())
                .propertyType(e.getDeal().getPropertyType().getName())
                .dueDate(e.getDueDate())
                .dueAmount(e.getDueAmount())
                .paidAmount(e.getPaidAmount())
                .balanceDue(e.getDueAmount().subtract(e.getPaidAmount()))
                .status(e.getStatus().name())
                .daysUntilDue(from.until(e.getDueDate()).getDays())
                .build()
        ).collect(Collectors.toList());

        BigDecimal total = items.stream()
                .map(UpcomingEmiItem::getBalanceDue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return UpcomingEmiWindow.builder()
                .count(items.size())
                .totalDueAmount(total)
                .items(items)
                .build();
    }

    // ── Overdue / Aging analysis ─────────────────────────────────────────────

    private OverdueAnalysisResponse buildOverdueAnalysis(LocalDate today) {
        List<EmiSchedule> allOverdue = emiScheduleRepository.findAllOverdue(today);

        AgingBucket b1 = buildBucket("1-30 Days",  allOverdue, today, 1,  30);
        AgingBucket b2 = buildBucket("31-60 Days", allOverdue, today, 31, 60);
        AgingBucket b3 = buildBucket("61-90 Days", allOverdue, today, 61, 90);
        AgingBucket b4 = buildBucket("90+ Days (NPA)", allOverdue, today, 91, Integer.MAX_VALUE);

        BigDecimal totalAmount = b1.getTotalAmount()
                .add(b2.getTotalAmount())
                .add(b3.getTotalAmount())
                .add(b4.getTotalAmount());

        return OverdueAnalysisResponse.builder()
                .totalOverdueCount(allOverdue.size())
                .totalOverdueAmount(totalAmount)
                .days1To30(b1)
                .days31To60(b2)
                .days61To90(b3)
                .days90Plus(b4)
                .build();
    }

    private AgingBucket buildBucket(String label, List<EmiSchedule> all,
                                    LocalDate today, int minDays, int maxDays) {
        List<EmiSchedule> filtered = all.stream().filter(e -> {
            long days = e.getDueDate().until(today).getDays();
            return days >= minDays && days <= maxDays;
        }).collect(Collectors.toList());

        List<OverdueEmiItem> items = filtered.stream().map(e -> {
            long daysOverdue = e.getDueDate().until(today).getDays();
            return OverdueEmiItem.builder()
                    .emiId(e.getId())
                    .dealId(e.getDeal().getId())
                    .customerName(e.getDeal().getCustomer().getFullName())
                    .customerPhone(e.getDeal().getCustomer().getPhoneNumber())
                    .propertyType(e.getDeal().getPropertyType().getName())
                    .dueDate(e.getDueDate())
                    .dueAmount(e.getDueAmount())
                    .paidAmount(e.getPaidAmount())
                    .balanceDue(e.getDueAmount().subtract(e.getPaidAmount()))
                    .status(e.getStatus().name())
                    .daysOverdue(daysOverdue)
                    .build();
        }).collect(Collectors.toList());

        BigDecimal total = items.stream()
                .map(OverdueEmiItem::getBalanceDue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return AgingBucket.builder()
                .label(label)
                .count(items.size())
                .totalAmount(total)
                .items(items)
                .build();
    }

    // ── Monthly trend (last 12 months) ───────────────────────────────────────

    private List<MonthlyTrendItem> buildMonthlyTrend(LocalDate today) {
        List<MonthlyTrendItem> trend = new ArrayList<>();
        YearMonth current = YearMonth.of(today.getYear(), today.getMonthValue());

        for (int i = 11; i >= 0; i--) {
            YearMonth ym = current.minusMonths(i);
            int y = ym.getYear();
            int m = ym.getMonthValue();

            long paid    = emiScheduleRepository.countByStatusAndMonth(EmiStatus.PAID, y, m);
            long pending = emiScheduleRepository.countByStatusAndMonth(EmiStatus.PENDING, y, m);
            long partial = emiScheduleRepository.countByStatusAndMonth(EmiStatus.PARTIAL, y, m);

            BigDecimal collected = coalesce(paymentRepository.sumByMonth(y, m));
            BigDecimal outstanding = coalesce(emiScheduleRepository.sumOutstandingByMonth(y, m));

            BigDecimal totalDue = collected.add(outstanding);
            double rate = totalDue.compareTo(BigDecimal.ZERO) == 0 ? 0.0
                    : collected.multiply(BigDecimal.valueOf(100))
                               .divide(totalDue, 2, RoundingMode.HALF_UP)
                               .doubleValue();

            String monthLabel = ym.getMonth().getDisplayName(TextStyle.SHORT, Locale.ENGLISH)
                    + " " + y;

            trend.add(MonthlyTrendItem.builder()
                    .year(y).month(m).monthLabel(monthLabel)
                    .emisPaid(paid).emisPending(pending).emisPartial(partial)
                    .amountCollected(collected)
                    .amountPending(outstanding)
                    .collectionRatePercent(rate)
                    .build());
        }
        return trend;
    }

    // ── Deal distribution ────────────────────────────────────────────────────

    private DealDistributionResponse buildDealDistribution() {
        long totalDeals = dealRepository.count();
        BigDecimal totalValue = coalesce(dealRepository.sumTotalAmount());

        List<Object[]> rows = dealRepository.distributionByPropertyType();
        List<DealDistributionResponse.PropertyTypeBreakdown> byType = rows.stream().map(r -> {
            String name  = (String) r[0];
            long count   = ((Number) r[1]).longValue();
            BigDecimal v = coalesce((BigDecimal) r[2]);
            double pct   = totalValue.compareTo(BigDecimal.ZERO) == 0 ? 0.0
                    : v.multiply(BigDecimal.valueOf(100))
                       .divide(totalValue, 2, RoundingMode.HALF_UP)
                       .doubleValue();
            return DealDistributionResponse.PropertyTypeBreakdown.builder()
                    .propertyType(name).dealCount(count).totalValue(v)
                    .percentOfPortfolio(pct).build();
        }).collect(Collectors.toList());

        List<DealDistributionResponse.StatusBreakdown> byStatus = List.of(
                statusBreakdown("ACTIVE",    dealRepository.countByStatus(DealStatus.ACTIVE),    totalDeals),
                statusBreakdown("COMPLETED", dealRepository.countByStatus(DealStatus.COMPLETED), totalDeals),
                statusBreakdown("DEFAULTED", dealRepository.countByStatus(DealStatus.DEFAULTED), totalDeals)
        );

        return DealDistributionResponse.builder()
                .byPropertyType(byType)
                .byStatus(byStatus)
                .build();
    }

    private DealDistributionResponse.StatusBreakdown statusBreakdown(String status, long count, long total) {
        double pct = total == 0 ? 0.0 : round2((double) count / total * 100);
        return DealDistributionResponse.StatusBreakdown.builder()
                .status(status).count(count).percentOfTotal(pct).build();
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private BigDecimal coalesce(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    private double round2(double v) {
        return BigDecimal.valueOf(v).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }
}
