package com.realestate.emi.service;

import com.realestate.emi.dto.request.SalaryPaymentRequest;
import com.realestate.emi.dto.response.SalaryRecordResponse;
import com.realestate.emi.entity.Organization;
import com.realestate.emi.entity.SalaryRecord;
import com.realestate.emi.entity.Staff;
import com.realestate.emi.enums.AttendanceStatus;
import com.realestate.emi.enums.SalaryStatus;
import com.realestate.emi.exception.ResourceNotFoundException;
import com.realestate.emi.exception.ServiceException;
import com.realestate.emi.repository.AttendanceRepository;
import com.realestate.emi.repository.OrganizationRepository;
import com.realestate.emi.repository.SalaryRecordRepository;
import com.realestate.emi.repository.StaffRepository;
import com.realestate.emi.security.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SalaryService {

    private final SalaryRecordRepository salaryRecordRepository;
    private final StaffRepository staffRepository;
    private final AttendanceRepository attendanceRepository;
    private final TenantContext tenantContext;
    private final OrganizationRepository organizationRepository;

    // Salary structure percentages (Indian construction industry standard)
    private static final BigDecimal BASIC_PAY_PERCENT = new BigDecimal("0.40");
    private static final BigDecimal HRA_PERCENT = new BigDecimal("0.25");
    private static final BigDecimal CONVEYANCE_PERCENT = new BigDecimal("0.10");
    private static final BigDecimal MAX_CONVEYANCE = new BigDecimal("1600");

    // Statutory thresholds
    private static final BigDecimal PF_RATE = new BigDecimal("0.12");
    private static final BigDecimal PF_SALARY_LIMIT = new BigDecimal("15000");
    private static final BigDecimal ESI_RATE = new BigDecimal("0.0075");
    private static final BigDecimal ESI_GROSS_LIMIT = new BigDecimal("21000");
    private static final BigDecimal PROFESSIONAL_TAX = new BigDecimal("200");
    private static final BigDecimal PT_SALARY_THRESHOLD = new BigDecimal("10000");

    @Transactional
    public List<SalaryRecordResponse> generateMonthlySalary(int year, int month) {
        log.info("Generating salary records for {}/{}", year, month);
        Long orgId = tenantContext.getCurrentOrganizationId();
        Organization org = organizationRepository.findById(orgId)
                .orElseThrow(() -> new ServiceException("Organization not found", "ORG_NOT_FOUND"));
        List<Staff> activeStaff = staffRepository.findByOrganizationIdAndIsActiveTrueOrderByFullNameAsc(orgId);
        YearMonth ym = YearMonth.of(year, month);
        int totalDaysInMonth = ym.lengthOfMonth();

        // Count Sundays
        int sundays = 0;
        for (int d = 1; d <= totalDaysInMonth; d++) {
            if (ym.atDay(d).getDayOfWeek() == DayOfWeek.SUNDAY) {
                sundays++;
            }
        }

        // Holiday-aware working days (graceful fallback if Holiday entity doesn't exist)
        int holidayCount = 0;
        try {
            holidayCount = countMandatoryHolidays(orgId, ym);
        } catch (Exception e) {
            log.debug("Holiday lookup not available, falling back to 0 holidays: {}", e.getMessage());
        }

        int workingDays = totalDaysInMonth - sundays - holidayCount;
        if (workingDays <= 0) workingDays = 1; // safety

        List<SalaryRecordResponse> results = new ArrayList<>();
        final int finalWorkingDays = workingDays;
        final int finalHolidayCount = holidayCount;

        for (Staff staff : activeStaff) {
            if (salaryRecordRepository.findByStaffIdAndYearAndMonth(staff.getId(), year, month).isPresent()) {
                continue; // Already generated
            }

            long presentDays = attendanceRepository.countByStaffAndStatusAndMonth(
                    staff.getId(), AttendanceStatus.PRESENT, year, month);
            long halfDays = attendanceRepository.countByStaffAndStatusAndMonth(
                    staff.getId(), AttendanceStatus.HALF_DAY, year, month);
            long absentDays = attendanceRepository.countByStaffAndStatusAndMonth(
                    staff.getId(), AttendanceStatus.ABSENT, year, month);
            Double overtimeHrs = attendanceRepository.sumOvertimeByStaffAndMonth(staff.getId(), year, month);

            BigDecimal monthlySalary = staff.getMonthlySalary();

            // ── Salary structure breakdown (full-month values) ──────────────
            BigDecimal basicPay = monthlySalary.multiply(BASIC_PAY_PERCENT).setScale(2, RoundingMode.HALF_UP);
            BigDecimal hra = monthlySalary.multiply(HRA_PERCENT).setScale(2, RoundingMode.HALF_UP);
            BigDecimal conveyanceAllowance = monthlySalary.multiply(CONVEYANCE_PERCENT)
                    .min(MAX_CONVEYANCE).setScale(2, RoundingMode.HALF_UP);
            BigDecimal specialAllowance = monthlySalary.subtract(basicPay).subtract(hra)
                    .subtract(conveyanceAllowance).setScale(2, RoundingMode.HALF_UP);

            // Effective days = present + (halfDays * 0.5)
            BigDecimal effectiveDays = BigDecimal.valueOf(presentDays)
                    .add(BigDecimal.valueOf(halfDays).multiply(new BigDecimal("0.5")));
            BigDecimal wdBd = BigDecimal.valueOf(finalWorkingDays);

            // ── Earned amounts (proportional to attendance) ─────────────────
            BigDecimal earnedBasicPay = basicPay.multiply(effectiveDays)
                    .divide(wdBd, 2, RoundingMode.HALF_UP);
            BigDecimal earnedHra = hra.multiply(effectiveDays)
                    .divide(wdBd, 2, RoundingMode.HALF_UP);
            BigDecimal earnedConveyance = conveyanceAllowance.multiply(effectiveDays)
                    .divide(wdBd, 2, RoundingMode.HALF_UP);
            BigDecimal earnedSpecial = specialAllowance.multiply(effectiveDays)
                    .divide(wdBd, 2, RoundingMode.HALF_UP);

            // Overtime pay = (perDaySalary / 8) * overtimeHours * 1.5
            BigDecimal perDaySalary = monthlySalary
                    .divide(wdBd, 2, RoundingMode.HALF_UP);
            BigDecimal overtimePay = BigDecimal.ZERO;
            if (overtimeHrs != null && overtimeHrs > 0) {
                BigDecimal perHourRate = perDaySalary.divide(BigDecimal.valueOf(8), 2, RoundingMode.HALF_UP);
                overtimePay = perHourRate
                        .multiply(BigDecimal.valueOf(overtimeHrs))
                        .multiply(new BigDecimal("1.5"))
                        .setScale(2, RoundingMode.HALF_UP);
            }

            // ── Gross earnings ──────────────────────────────────────────────
            BigDecimal grossEarnings = earnedBasicPay.add(earnedHra).add(earnedConveyance)
                    .add(earnedSpecial).add(overtimePay);

            // ── Statutory deductions (on earned amounts) ────────────────────
            BigDecimal pfDeduction = BigDecimal.ZERO;
            if (monthlySalary.compareTo(PF_SALARY_LIMIT) <= 0) {
                pfDeduction = earnedBasicPay.multiply(PF_RATE).setScale(2, RoundingMode.HALF_UP);
            }

            BigDecimal esiDeduction = BigDecimal.ZERO;
            if (grossEarnings.compareTo(ESI_GROSS_LIMIT) <= 0) {
                esiDeduction = grossEarnings.multiply(ESI_RATE).setScale(2, RoundingMode.HALF_UP);
            }

            BigDecimal professionalTax = BigDecimal.ZERO;
            if (monthlySalary.compareTo(PT_SALARY_THRESHOLD) > 0) {
                professionalTax = PROFESSIONAL_TAX;
            }

            // ── Advance auto-deduction ──────────────────────────────────────
            BigDecimal advanceDeduction = BigDecimal.ZERO;
            try {
                advanceDeduction = processAdvanceDeductions(staff.getId());
            } catch (Exception e) {
                log.debug("Advance deduction lookup not available: {}", e.getMessage());
            }

            // ── Total deductions and net salary ─────────────────────────────
            BigDecimal totalDeductions = pfDeduction.add(esiDeduction)
                    .add(professionalTax).add(advanceDeduction);
            BigDecimal netSalary = grossEarnings.subtract(totalDeductions)
                    .max(BigDecimal.ZERO);

            SalaryRecord record = SalaryRecord.builder()
                    .staff(staff)
                    .year(year)
                    .month(month)
                    .baseSalary(monthlySalary)
                    .workingDays(finalWorkingDays)
                    .presentDays((int) presentDays)
                    .halfDays((int) halfDays)
                    .absentDays((int) absentDays)
                    .overtimeHours(BigDecimal.valueOf(overtimeHrs != null ? overtimeHrs : 0))
                    .overtimePay(overtimePay)
                    .basicPay(earnedBasicPay)
                    .hra(earnedHra)
                    .conveyanceAllowance(earnedConveyance)
                    .specialAllowance(earnedSpecial)
                    .pfDeduction(pfDeduction)
                    .esiDeduction(esiDeduction)
                    .professionalTax(professionalTax)
                    .advanceDeduction(advanceDeduction)
                    .grossEarnings(grossEarnings)
                    .totalDeductions(totalDeductions)
                    .netSalary(netSalary)
                    .holidayCount(finalHolidayCount)
                    .status(SalaryStatus.PENDING)
                    .build();

            record = salaryRecordRepository.save(record);
            results.add(toResponse(record));
            log.info("Generated salary for {} ({}/{}): gross={}, deductions={}, net={}",
                    staff.getFullName(), year, month, grossEarnings, totalDeductions, netSalary);
        }

        return results;
    }

    @Transactional
    public SalaryRecordResponse paySalary(Long salaryId, SalaryPaymentRequest request) {
        SalaryRecord record = salaryRecordRepository.findById(salaryId)
                .orElseThrow(() -> new ResourceNotFoundException("SalaryRecord", salaryId));

        if (record.getStatus() == SalaryStatus.PAID) {
            throw new ServiceException("Salary is already fully paid", "SALARY_ALREADY_PAID");
        }

        record.setBonus(request.getBonus() != null ? request.getBonus() : BigDecimal.ZERO);
        record.setDeductions(request.getDeductions() != null ? request.getDeductions() : BigDecimal.ZERO);

        // Recalculate net with bonus/deductions
        BigDecimal grossWithBonus = record.getGrossEarnings().add(record.getBonus());
        BigDecimal allDeductions = record.getTotalDeductions().add(record.getDeductions());
        BigDecimal netSalary = grossWithBonus.subtract(allDeductions).max(BigDecimal.ZERO);
        record.setNetSalary(netSalary);

        BigDecimal newPaid = record.getAmountPaid().add(request.getAmount());
        record.setAmountPaid(newPaid);
        record.setRemarks(request.getRemarks());
        record.setPaymentDate(LocalDate.now());

        // Payment tracking
        if (request.getPaymentMethod() != null) {
            record.setPaymentMethod(request.getPaymentMethod());
        }
        if (request.getPaymentReference() != null) {
            record.setPaymentReference(request.getPaymentReference());
        }

        if (newPaid.compareTo(netSalary) >= 0) {
            record.setStatus(SalaryStatus.PAID);
        } else {
            record.setStatus(SalaryStatus.PARTIAL);
        }

        record = salaryRecordRepository.save(record);
        log.info("Paid salary {} for staff {}: amount={}, method={}, status={}",
                salaryId, record.getStaff().getFullName(), request.getAmount(),
                request.getPaymentMethod(), record.getStatus());
        return toResponse(record);
    }

    @Transactional(readOnly = true)
    public List<SalaryRecordResponse> getSalaryByMonth(int year, int month) {
        Long orgId = tenantContext.getCurrentOrganizationId();
        return salaryRecordRepository.findByYearAndMonthAndStaffOrganizationIdOrderByStaffFullNameAsc(year, month, orgId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<SalaryRecordResponse> getSalaryByStaff(Long staffId) {
        Long orgId = tenantContext.getCurrentOrganizationId();
        return salaryRecordRepository.findByStaffIdAndStaffOrganizationIdOrderByYearDescMonthDesc(staffId, orgId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<SalaryRecordResponse> getPendingSalaries() {
        Long orgId = tenantContext.getCurrentOrganizationId();
        return salaryRecordRepository.findByStatusAndOrg(SalaryStatus.PENDING, orgId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get salary records as entities for payroll Excel export.
     */
    @Transactional(readOnly = true)
    public List<SalaryRecord> getSalaryRecordsByMonth(int year, int month) {
        Long orgId = tenantContext.getCurrentOrganizationId();
        return salaryRecordRepository.findByYearAndMonthAndStaffOrganizationIdOrderByStaffFullNameAsc(year, month, orgId);
    }

    /**
     * Get a single salary record entity (for PDF slip).
     */
    @Transactional(readOnly = true)
    public SalaryRecord getSalaryRecordById(Long salaryId) {
        return salaryRecordRepository.findById(salaryId)
                .orElseThrow(() -> new ResourceNotFoundException("SalaryRecord", salaryId));
    }

    // ── Holiday lookup (graceful — works even if Holiday entity doesn't exist) ──

    private int countMandatoryHolidays(Long orgId, YearMonth ym) {
        // Holiday entity may not exist yet; return 0 gracefully
        // When Holiday module is added, inject HolidayRepository and query:
        // countByOrganizationIdAndDateBetweenAndIsOptionalFalse(orgId, ym.atDay(1), ym.atEndOfMonth())
        return 0;
    }

    // ── Advance deduction processing (graceful — works even without SalaryAdvance entity) ──

    private BigDecimal processAdvanceDeductions(Long staffId) {
        // SalaryAdvance entity may not exist yet; return 0 gracefully
        // When Salary Advance module is added, inject SalaryAdvanceRepository and:
        // 1. Query active advances for staff
        // 2. For each: deduct min(monthlyDeductionAmount, balanceRemaining)
        // 3. Update advance.balanceRemaining; if 0, set status = FULLY_DEDUCTED
        // 4. Return total advance deduction
        return BigDecimal.ZERO;
    }

    private SalaryRecordResponse toResponse(SalaryRecord record) {
        BigDecimal balanceDue = record.getNetSalary().subtract(record.getAmountPaid());
        if (balanceDue.compareTo(BigDecimal.ZERO) < 0) balanceDue = BigDecimal.ZERO;

        return SalaryRecordResponse.builder()
                .id(record.getId())
                .staffId(record.getStaff().getId())
                .staffName(record.getStaff().getFullName())
                .staffRole(record.getStaff().getStaffRole().getName())
                .year(record.getYear())
                .month(record.getMonth())
                .baseSalary(record.getBaseSalary())
                .workingDays(record.getWorkingDays())
                .presentDays(record.getPresentDays())
                .halfDays(record.getHalfDays())
                .absentDays(record.getAbsentDays())
                .overtimeHours(record.getOvertimeHours())
                .overtimePay(record.getOvertimePay())
                .deductions(record.getDeductions())
                .bonus(record.getBonus())
                .basicPay(record.getBasicPay())
                .hra(record.getHra())
                .conveyanceAllowance(record.getConveyanceAllowance())
                .specialAllowance(record.getSpecialAllowance())
                .pfDeduction(record.getPfDeduction())
                .esiDeduction(record.getEsiDeduction())
                .professionalTax(record.getProfessionalTax())
                .advanceDeduction(record.getAdvanceDeduction())
                .grossEarnings(record.getGrossEarnings())
                .totalDeductions(record.getTotalDeductions())
                .netSalary(record.getNetSalary())
                .amountPaid(record.getAmountPaid())
                .balanceDue(balanceDue)
                .status(record.getStatus())
                .paymentDate(record.getPaymentDate())
                .remarks(record.getRemarks())
                .paymentMethod(record.getPaymentMethod())
                .paymentReference(record.getPaymentReference())
                .holidayCount(record.getHolidayCount())
                .build();
    }
}
