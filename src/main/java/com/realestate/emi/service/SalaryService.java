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

    @Transactional
    public List<SalaryRecordResponse> generateMonthlySalary(int year, int month) {
        log.info("Generating salary records for {}/{}", year, month);
        Long orgId = tenantContext.getCurrentOrganizationId();
        Organization org = organizationRepository.findById(orgId)
                .orElseThrow(() -> new ServiceException("Organization not found", "ORG_NOT_FOUND"));
        List<Staff> activeStaff = staffRepository.findByOrganizationIdAndIsActiveTrueOrderByFullNameAsc(orgId);
        YearMonth ym = YearMonth.of(year, month);
        int totalDaysInMonth = ym.lengthOfMonth();

        // Assume ~26 working days per month (exclude Sundays)
        int workingDays = 0;
        for (int d = 1; d <= totalDaysInMonth; d++) {
            LocalDate date = ym.atDay(d);
            if (date.getDayOfWeek().getValue() != 7) { // Not Sunday
                workingDays++;
            }
        }

        List<SalaryRecordResponse> results = new ArrayList<>();
        final int finalWorkingDays = workingDays;

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

            BigDecimal perDaySalary = staff.getMonthlySalary()
                    .divide(BigDecimal.valueOf(finalWorkingDays), 2, RoundingMode.HALF_UP);

            // Effective days = present + (halfDays * 0.5)
            BigDecimal effectiveDays = BigDecimal.valueOf(presentDays)
                    .add(BigDecimal.valueOf(halfDays).multiply(new BigDecimal("0.5")));

            BigDecimal earnedSalary = perDaySalary.multiply(effectiveDays).setScale(2, RoundingMode.HALF_UP);

            // Overtime pay = (perDaySalary / 8) * overtimeHours * 1.5
            BigDecimal overtimePay = BigDecimal.ZERO;
            if (overtimeHrs != null && overtimeHrs > 0) {
                BigDecimal perHourRate = perDaySalary.divide(BigDecimal.valueOf(8), 2, RoundingMode.HALF_UP);
                overtimePay = perHourRate
                        .multiply(BigDecimal.valueOf(overtimeHrs))
                        .multiply(new BigDecimal("1.5"))
                        .setScale(2, RoundingMode.HALF_UP);
            }

            BigDecimal netSalary = earnedSalary.add(overtimePay);

            SalaryRecord record = SalaryRecord.builder()
                    .staff(staff)
                    .year(year)
                    .month(month)
                    .baseSalary(staff.getMonthlySalary())
                    .workingDays(finalWorkingDays)
                    .presentDays((int) presentDays)
                    .halfDays((int) halfDays)
                    .absentDays((int) absentDays)
                    .overtimeHours(BigDecimal.valueOf(overtimeHrs != null ? overtimeHrs : 0))
                    .overtimePay(overtimePay)
                    .netSalary(netSalary)
                    .status(SalaryStatus.PENDING)
                    .build();

            record = salaryRecordRepository.save(record);
            results.add(toResponse(record));
            log.info("Generated salary for {} ({}/{}): net={}", staff.getFullName(), year, month, netSalary);
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
        BigDecimal netSalary = record.getNetSalary()
                .add(record.getBonus())
                .subtract(record.getDeductions());
        record.setNetSalary(netSalary);

        BigDecimal newPaid = record.getAmountPaid().add(request.getAmount());
        record.setAmountPaid(newPaid);
        record.setRemarks(request.getRemarks());
        record.setPaymentDate(LocalDate.now());

        if (newPaid.compareTo(netSalary) >= 0) {
            record.setStatus(SalaryStatus.PAID);
        } else {
            record.setStatus(SalaryStatus.PARTIAL);
        }

        record = salaryRecordRepository.save(record);
        log.info("Paid salary {} for staff {}: amount={}, status={}",
                salaryId, record.getStaff().getFullName(), request.getAmount(), record.getStatus());
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
                .netSalary(record.getNetSalary())
                .amountPaid(record.getAmountPaid())
                .balanceDue(balanceDue)
                .status(record.getStatus())
                .paymentDate(record.getPaymentDate())
                .remarks(record.getRemarks())
                .build();
    }
}
