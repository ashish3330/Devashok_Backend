package com.realestate.emi.service;

import com.realestate.emi.dto.response.*;
import com.realestate.emi.entity.EmiSchedule;
import com.realestate.emi.entity.SalaryRecord;
import com.realestate.emi.entity.Staff;
import com.realestate.emi.entity.Supplier;
import com.realestate.emi.enums.DealStatus;
import com.realestate.emi.enums.EmiStatus;
import com.realestate.emi.enums.MaterialCategory;
import com.realestate.emi.enums.TransactionType;
import com.realestate.emi.repository.*;
import com.realestate.emi.security.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardService {

    private final DealRepository dealRepository;
    private final PaymentRepository paymentRepository;
    private final EmiScheduleRepository emiScheduleRepository;
    private final CustomerRepository customerRepository;
    private final SalaryRecordRepository salaryRecordRepository;
    private final StaffRepository staffRepository;
    private final MaterialRepository materialRepository;
    private final StockTransactionRepository stockTransactionRepository;
    private final SupplierPaymentRepository supplierPaymentRepository;
    private final SupplierRepository supplierRepository;
    private final SalaryAdvanceRepository salaryAdvanceRepository;
    private final AttendanceRepository attendanceRepository;
    private final TenantContext tenantContext;
    private final OrganizationRepository organizationRepository;

    // ── Legacy summary (kept for backward compat) ───────────────────────────

    @Transactional(readOnly = true)
    public DashboardResponse getSummary() {
        Long orgId = tenantContext.getCurrentOrganizationId();
        long active = dealRepository.countByStatusAndOrganizationId(DealStatus.ACTIVE, orgId);
        long completed = dealRepository.countByStatusAndOrganizationId(DealStatus.COMPLETED, orgId);
        long defaulted = dealRepository.countByStatusAndOrganizationId(DealStatus.DEFAULTED, orgId);
        return DashboardResponse.builder()
                .totalDeals(active + completed + defaulted)
                .activeDeals(active)
                .completedDeals(completed)
                .defaultedDeals(defaulted)
                .totalDealAmount(dealRepository.sumTotalPayableAfterDepositByOrg(orgId))
                .totalReceived(paymentRepository.sumAllPaymentsByOrg(orgId))
                .totalOutstanding(emiScheduleRepository.sumTotalOutstandingByOrg(orgId))
                .build();
    }

    // ── Monthly analytics ────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public MonthlyAnalyticsResponse getMonthlyAnalytics(int year, int month) {
        Long orgId = tenantContext.getCurrentOrganizationId();
        long completed = emiScheduleRepository.countByStatusAndMonthAndOrg(EmiStatus.PAID, year, month, orgId);
        long pending   = emiScheduleRepository.countByStatusAndMonthAndOrg(EmiStatus.PENDING, year, month, orgId);
        long partial   = emiScheduleRepository.countByStatusAndMonthAndOrg(EmiStatus.PARTIAL, year, month, orgId);

        return MonthlyAnalyticsResponse.builder()
                .year(year).month(month)
                .paymentsCompleted(completed)
                .paymentsPending(pending)
                .paymentsPartial(partial)
                .amountReceived(paymentRepository.sumByMonthAndOrg(year, month, orgId))
                .amountPending(emiScheduleRepository.sumOutstandingByMonthAndOrg(year, month, orgId))
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

    // ── Expense dashboard ────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public ExpenseDashboardResponse getExpenseDashboard(int year, int month) {
        log.debug("Building expense dashboard for {}/{}", year, month);
        Long orgId = tenantContext.getCurrentOrganizationId();

        // ── Salary data ─────────────────────────────────────────────────────
        BigDecimal totalMonthlySalaryBudget = coalesce(staffRepository.sumTotalMonthlySalaryByOrg(orgId));
        BigDecimal grossPayroll = coalesce(salaryRecordRepository.sumGrossPayrollByMonthAndOrg(year, month, orgId));
        BigDecimal netPayroll = coalesce(salaryRecordRepository.sumNetSalaryByMonthAndOrg(year, month, orgId));
        BigDecimal totalSalaryPaid = coalesce(salaryRecordRepository.sumPaidByMonthAndOrg(year, month, orgId));
        BigDecimal pendingSalary = netPayroll.subtract(totalSalaryPaid);
        if (pendingSalary.compareTo(BigDecimal.ZERO) < 0) pendingSalary = BigDecimal.ZERO;
        BigDecimal overtimePay = coalesce(salaryRecordRepository.sumOvertimePayByMonthAndOrg(year, month, orgId));
        BigDecimal advancesGiven = coalesce(salaryAdvanceRepository.sumAdvancesGivenByMonthAndOrg(year, month, orgId));

        List<Staff> activeStaff = staffRepository.findByOrganizationIdAndIsActiveTrueOrderByFullNameAsc(orgId);
        int staffCount = activeStaff.size();
        BigDecimal avgSalary = staffCount > 0
                ? totalMonthlySalaryBudget.divide(BigDecimal.valueOf(staffCount), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        long pendingSalaryCount = salaryRecordRepository.findByStatusAndOrg(
                com.realestate.emi.enums.SalaryStatus.PENDING, orgId).size();

        // ── Material / Inventory data ───────────────────────────────────────
        BigDecimal totalPurchases = coalesce(stockTransactionRepository.sumInwardCostByMonthAndOrg(year, month, orgId));
        BigDecimal materialOutward = coalesce(stockTransactionRepository.sumOutwardCostByMonthAndOrg(year, month, orgId));
        BigDecimal totalWastage = coalesce(stockTransactionRepository.sumCostByTypeAndMonthAndOrg(
                TransactionType.ADJUSTMENT, year, month, orgId));
        BigDecimal totalInventoryValue = coalesce(materialRepository.calculateTotalInventoryValueByOrg(orgId));
        long totalMaterials = materialRepository.findByOrganizationIdAndIsActiveTrueOrderByNameAsc(orgId).size();
        long lowStockAlerts = materialRepository.findLowStockMaterialsByOrg(orgId).size();
        long outOfStockCount = materialRepository.findOutOfStockMaterialsByOrg(orgId).size();

        // Supplier payments
        BigDecimal supplierPaymentsMonth = coalesce(supplierPaymentRepository.sumPaymentsByMonthAndOrg(year, month, orgId));
        BigDecimal totalInwardCostAll = coalesce(stockTransactionRepository.sumTotalInwardCostByOrg(orgId));
        BigDecimal totalPaidToSuppliers = coalesce(supplierPaymentRepository.sumTotalPaidByOrg(orgId));
        BigDecimal outstandingSupplierBalance = totalInwardCostAll.subtract(totalPaidToSuppliers);
        if (outstandingSupplierBalance.compareTo(BigDecimal.ZERO) < 0) outstandingSupplierBalance = BigDecimal.ZERO;

        // Total expenses
        BigDecimal totalExpenses = totalSalaryPaid.add(totalPurchases).add(supplierPaymentsMonth);
        BigDecimal salaryExpenses = totalSalaryPaid;
        BigDecimal materialExpenses = totalPurchases;

        // ── Previous month comparison ───────────────────────────────────────
        YearMonth prevYm = YearMonth.of(year, month).minusMonths(1);
        BigDecimal prevSalary = coalesce(salaryRecordRepository.sumPaidByMonthAndOrg(prevYm.getYear(), prevYm.getMonthValue(), orgId));
        BigDecimal prevMaterial = coalesce(stockTransactionRepository.sumInwardCostByMonthAndOrg(prevYm.getYear(), prevYm.getMonthValue(), orgId));
        BigDecimal prevSupplier = coalesce(supplierPaymentRepository.sumPaymentsByMonthAndOrg(prevYm.getYear(), prevYm.getMonthValue(), orgId));
        BigDecimal previousMonthTotal = prevSalary.add(prevMaterial).add(prevSupplier);
        double monthOverMonthChange = previousMonthTotal.compareTo(BigDecimal.ZERO) == 0 ? 0.0
                : totalExpenses.subtract(previousMonthTotal)
                        .multiply(BigDecimal.valueOf(100))
                        .divide(previousMonthTotal, 2, RoundingMode.HALF_UP)
                        .doubleValue();

        // ── Category breakdown ──────────────────────────────────────────────
        List<Object[]> catRows = stockTransactionRepository.sumInwardCostByCategoryAndMonthAndOrg(year, month, orgId);
        List<ExpenseDashboardResponse.CategoryExpense> categoryBreakdown = catRows.stream().map(r -> {
            String catName = r[0] != null ? r[0].toString() : "OTHER";
            BigDecimal amt = coalesce((BigDecimal) r[1]);
            return ExpenseDashboardResponse.CategoryExpense.builder()
                    .category(catName).amount(amt).build();
        }).collect(Collectors.toList());

        // ── 6-month trends ──────────────────────────────────────────────────
        List<ExpenseDashboardResponse.MonthlyExpenseTrend> trends = new ArrayList<>();
        List<ExpenseDashboardResponse.MonthlyExpenseItem> legacyTrend = new ArrayList<>();
        YearMonth current = YearMonth.of(year, month);
        for (int i = 5; i >= 0; i--) {
            YearMonth ym = current.minusMonths(i);
            int y = ym.getYear();
            int m = ym.getMonthValue();
            BigDecimal salaryExp = coalesce(salaryRecordRepository.sumPaidByMonthAndOrg(y, m, orgId));
            BigDecimal materialExp = coalesce(stockTransactionRepository.sumInwardCostByMonthAndOrg(y, m, orgId));
            BigDecimal supplierExp = coalesce(supplierPaymentRepository.sumPaymentsByMonthAndOrg(y, m, orgId));
            String monthLabel = ym.getMonth().getDisplayName(TextStyle.SHORT, Locale.ENGLISH) + " " + y;

            trends.add(ExpenseDashboardResponse.MonthlyExpenseTrend.builder()
                    .year(y).month(m).monthLabel(monthLabel)
                    .salaryExpense(salaryExp)
                    .materialExpense(materialExp)
                    .supplierPayment(supplierExp)
                    .totalExpense(salaryExp.add(materialExp).add(supplierExp))
                    .build());

            legacyTrend.add(ExpenseDashboardResponse.MonthlyExpenseItem.builder()
                    .year(y).month(m).monthLabel(monthLabel)
                    .salaryExpense(salaryExp)
                    .materialExpense(materialExp)
                    .totalExpense(salaryExp.add(materialExp))
                    .build());
        }

        // ── Top 5 staff by salary ───────────────────────────────────────────
        List<SalaryRecord> salaryRecords = salaryRecordRepository
                .findByYearAndMonthAndStaffOrganizationIdOrderByNetSalaryDesc(year, month, orgId);
        List<ExpenseDashboardResponse.TopExpenseItem> topStaff = salaryRecords.stream()
                .limit(5)
                .map(sr -> ExpenseDashboardResponse.TopExpenseItem.builder()
                        .name(sr.getStaff() != null ? sr.getStaff().getFullName() : "Unknown")
                        .amount(sr.getNetSalary())
                        .subtitle(sr.getStaff() != null && sr.getStaff().getStaffRole() != null
                                ? sr.getStaff().getStaffRole().getName() : "")
                        .build())
                .collect(Collectors.toList());

        // ── Top 5 materials by spend ────────────────────────────────────────
        List<Object[]> matRows = stockTransactionRepository.findTopMaterialsBySpendAndMonthAndOrg(year, month, orgId);
        List<ExpenseDashboardResponse.TopExpenseItem> topMaterials = matRows.stream()
                .limit(5)
                .map(r -> ExpenseDashboardResponse.TopExpenseItem.builder()
                        .name((String) r[0])
                        .amount(coalesce((BigDecimal) r[1]))
                        .subtitle("Purchase cost")
                        .build())
                .collect(Collectors.toList());

        // ── Top 5 suppliers by payment ──────────────────────────────────────
        List<Object[]> supRows = supplierPaymentRepository.findTopSuppliersByPaymentAndMonthAndOrg(year, month, orgId);
        List<ExpenseDashboardResponse.TopExpenseItem> topSuppliers = supRows.stream()
                .limit(5)
                .map(r -> ExpenseDashboardResponse.TopExpenseItem.builder()
                        .name((String) r[1])
                        .amount(coalesce((BigDecimal) r[2]))
                        .subtitle("Payments made")
                        .build())
                .collect(Collectors.toList());

        return ExpenseDashboardResponse.builder()
                // New summary fields
                .totalExpenses(totalExpenses)
                .salaryExpenses(salaryExpenses)
                .materialExpenses(materialExpenses)
                .supplierPayments(supplierPaymentsMonth)
                .previousMonthTotal(previousMonthTotal)
                .monthOverMonthChange(monthOverMonthChange)
                // Salary breakdown
                .staffCount(staffCount)
                .grossPayroll(grossPayroll)
                .netPayroll(netPayroll)
                .totalSalaryPaid(totalSalaryPaid)
                .pendingSalary(pendingSalary)
                .averageSalary(avgSalary)
                .totalOvertimePay(overtimePay)
                .totalAdvancesGiven(advancesGiven)
                // Material breakdown
                .totalPurchases(totalPurchases)
                .totalWastage(totalWastage)
                .totalDamage(BigDecimal.ZERO)
                .outstandingSupplierBalance(outstandingSupplierBalance)
                .lowStockAlerts(lowStockAlerts)
                .outOfStockCount(outOfStockCount)
                .categoryBreakdown(categoryBreakdown)
                // Trends
                .trends(trends)
                // Top items
                .topStaffBySalary(topStaff)
                .topMaterialsBySpend(topMaterials)
                .topSuppliersByPayment(topSuppliers)
                // Legacy fields for backward compatibility
                .totalMonthlySalaryBudget(totalMonthlySalaryBudget)
                .currentMonthSalaryPaid(totalSalaryPaid)
                .currentMonthSalaryPending(pendingSalary)
                .totalActiveStaff((long) staffCount)
                .pendingSalaryCount(pendingSalaryCount)
                .totalInventoryValue(totalInventoryValue)
                .totalMaterialExpenseThisMonth(materialOutward)
                .totalMaterials(totalMaterials)
                .totalExpensesThisMonth(totalExpenses)
                .expenseTrend(legacyTrend)
                .build();
    }

    // ── Staff analytics ─────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public StaffAnalyticsResponse getStaffAnalytics() {
        log.debug("Building staff analytics");
        Long orgId = tenantContext.getCurrentOrganizationId();
        LocalDate today = LocalDate.now();
        int year = today.getYear();
        int month = today.getMonthValue();

        List<Staff> allStaff = staffRepository.findByOrganizationIdOrderByFullNameAsc(orgId);
        List<Staff> activeStaff = allStaff.stream().filter(s -> Boolean.TRUE.equals(s.getIsActive())).collect(Collectors.toList());
        List<Staff> inactiveStaff = allStaff.stream().filter(s -> !Boolean.TRUE.equals(s.getIsActive())).collect(Collectors.toList());

        // Staff by department
        Map<String, Integer> byDepartment = new LinkedHashMap<>();
        for (Staff s : activeStaff) {
            String dept = s.getDepartment() != null && !s.getDepartment().isBlank()
                    ? s.getDepartment() : "Unassigned";
            byDepartment.merge(dept, 1, Integer::sum);
        }

        // Staff by employment type
        Map<String, Integer> byEmploymentType = new LinkedHashMap<>();
        for (Staff s : activeStaff) {
            String type = s.getEmploymentType() != null
                    ? s.getEmploymentType().name() : "PERMANENT";
            byEmploymentType.merge(type, 1, Integer::sum);
        }

        BigDecimal totalMonthlyPayroll = coalesce(staffRepository.sumTotalMonthlySalaryByOrg(orgId));

        // Average attendance rate this month
        YearMonth ym = YearMonth.of(year, month);
        int workingDaysInMonth = ym.lengthOfMonth(); // Simplified approximation
        BigDecimal totalAttendanceRate = BigDecimal.ZERO;
        int staffWithAttendance = 0;
        for (Staff s : activeStaff) {
            long presentDays = attendanceRepository.countByStaffAndStatusAndMonth(
                    s.getId(), com.realestate.emi.enums.AttendanceStatus.PRESENT, year, month);
            long halfDays = attendanceRepository.countByStaffAndStatusAndMonth(
                    s.getId(), com.realestate.emi.enums.AttendanceStatus.HALF_DAY, year, month);
            double effective = presentDays + (halfDays * 0.5);
            if (workingDaysInMonth > 0) {
                double rate = (effective / workingDaysInMonth) * 100;
                totalAttendanceRate = totalAttendanceRate.add(BigDecimal.valueOf(rate));
                staffWithAttendance++;
            }
        }
        BigDecimal avgAttendanceRate = staffWithAttendance > 0
                ? totalAttendanceRate.divide(BigDecimal.valueOf(staffWithAttendance), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        // Advances
        int advancesOutstanding = salaryAdvanceRepository.countActiveByOrg(orgId);
        BigDecimal totalAdvanceAmount = coalesce(salaryAdvanceRepository.sumActiveBalanceByOrg(orgId));

        // Department summary
        List<StaffAnalyticsResponse.DepartmentSummary> deptSummary = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : byDepartment.entrySet()) {
            String dept = entry.getKey();
            int count = entry.getValue();
            BigDecimal totalSalary = activeStaff.stream()
                    .filter(s -> dept.equals(s.getDepartment() != null && !s.getDepartment().isBlank()
                            ? s.getDepartment() : "Unassigned"))
                    .map(Staff::getMonthlySalary)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal avgSal = count > 0
                    ? totalSalary.divide(BigDecimal.valueOf(count), 2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;
            deptSummary.add(StaffAnalyticsResponse.DepartmentSummary.builder()
                    .department(dept)
                    .count(count)
                    .totalSalary(totalSalary)
                    .averageSalary(avgSal)
                    .build());
        }

        return StaffAnalyticsResponse.builder()
                .totalStaff(allStaff.size())
                .activeStaff(activeStaff.size())
                .inactiveStaff(inactiveStaff.size())
                .staffByDepartment(byDepartment)
                .staffByEmploymentType(byEmploymentType)
                .totalMonthlyPayroll(totalMonthlyPayroll)
                .averageAttendanceRate(avgAttendanceRate)
                .totalAdvancesOutstanding(advancesOutstanding)
                .totalAdvanceAmount(totalAdvanceAmount)
                .departmentSummary(deptSummary)
                .build();
    }

    // ── Portfolio overview ───────────────────────────────────────────────────

    private PortfolioOverviewResponse buildPortfolioOverview(LocalDate today) {
        Long orgId = tenantContext.getCurrentOrganizationId();
        long activeDeals    = dealRepository.countByStatusAndOrganizationId(DealStatus.ACTIVE, orgId);
        long completedDeals = dealRepository.countByStatusAndOrganizationId(DealStatus.COMPLETED, orgId);
        long defaultedDeals = dealRepository.countByStatusAndOrganizationId(DealStatus.DEFAULTED, orgId);
        long totalDeals     = activeDeals + completedDeals + defaultedDeals;

        BigDecimal totalCollected   = coalesce(paymentRepository.sumAllPaymentsByOrg(orgId));
        BigDecimal totalOutstanding = coalesce(emiScheduleRepository.sumTotalOutstandingByOrg(orgId));
        BigDecimal totalPortfolio   = coalesce(dealRepository.sumTotalPayableAfterDepositByOrg(orgId));

        BigDecimal totalPayable = totalCollected.add(totalOutstanding);
        double efficiency = totalPayable.compareTo(BigDecimal.ZERO) == 0 ? 0.0
                : totalCollected.multiply(BigDecimal.valueOf(100))
                                .divide(totalPayable, 2, RoundingMode.HALF_UP)
                                .doubleValue();

        long npaDeals = emiScheduleRepository.countNpaDealsByOrg(today.minusDays(90), orgId);
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
                .averageDealValue(coalesce(dealRepository.avgDealValueByOrg(orgId)))
                .averageEmiAmount(coalesce(dealRepository.avgEmiAmountByOrg(orgId)))
                .totalCustomers(customerRepository.countByOrganizationId(orgId))
                .build();
    }

    // ── Upcoming EMI windows ─────────────────────────────────────────────────

    private UpcomingEmiWindow buildUpcomingWindow(LocalDate from, LocalDate to, String label) {
        Long orgId = tenantContext.getCurrentOrganizationId();
        List<EmiSchedule> schedules = emiScheduleRepository.findUpcomingEmisByOrg(from, to, orgId);

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
        Long orgId = tenantContext.getCurrentOrganizationId();
        List<EmiSchedule> allOverdue = emiScheduleRepository.findAllOverdueByOrg(today, orgId);

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
        Long orgId = tenantContext.getCurrentOrganizationId();
        List<MonthlyTrendItem> trend = new ArrayList<>();
        YearMonth current = YearMonth.of(today.getYear(), today.getMonthValue());

        for (int i = 11; i >= 0; i--) {
            YearMonth ym = current.minusMonths(i);
            int y = ym.getYear();
            int m = ym.getMonthValue();

            long paid    = emiScheduleRepository.countByStatusAndMonthAndOrg(EmiStatus.PAID, y, m, orgId);
            long pending = emiScheduleRepository.countByStatusAndMonthAndOrg(EmiStatus.PENDING, y, m, orgId);
            long partial = emiScheduleRepository.countByStatusAndMonthAndOrg(EmiStatus.PARTIAL, y, m, orgId);

            BigDecimal collected = coalesce(paymentRepository.sumByMonthAndOrg(y, m, orgId));
            BigDecimal outstanding = coalesce(emiScheduleRepository.sumOutstandingByMonthAndOrg(y, m, orgId));

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
        Long orgId = tenantContext.getCurrentOrganizationId();
        long totalDeals = dealRepository.findAllByOrganization(orgId).size();
        BigDecimal totalValue = coalesce(dealRepository.sumTotalAmountByOrg(orgId));

        List<Object[]> rows = dealRepository.distributionByPropertyTypeAndOrg(orgId);
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
                statusBreakdown("ACTIVE",    dealRepository.countByStatusAndOrganizationId(DealStatus.ACTIVE, orgId),    totalDeals),
                statusBreakdown("COMPLETED", dealRepository.countByStatusAndOrganizationId(DealStatus.COMPLETED, orgId), totalDeals),
                statusBreakdown("DEFAULTED", dealRepository.countByStatusAndOrganizationId(DealStatus.DEFAULTED, orgId), totalDeals)
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
