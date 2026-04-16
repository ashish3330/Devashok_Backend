package com.realestate.emi.service;

import com.realestate.emi.dto.request.AttendanceRequest;
import com.realestate.emi.dto.response.AttendanceResponse;
import com.realestate.emi.dto.response.AttendanceSummaryResponse;
import com.realestate.emi.entity.Attendance;
import com.realestate.emi.entity.Holiday;
import com.realestate.emi.entity.Organization;
import com.realestate.emi.entity.Staff;
import com.realestate.emi.entity.User;
import com.realestate.emi.enums.AttendanceStatus;
import com.realestate.emi.enums.Role;
import com.realestate.emi.exception.ResourceNotFoundException;
import com.realestate.emi.exception.ServiceException;
import com.realestate.emi.repository.AttendanceRepository;
import com.realestate.emi.repository.HolidayRepository;
import com.realestate.emi.repository.OrganizationRepository;
import com.realestate.emi.repository.StaffRepository;
import com.realestate.emi.repository.UserRepository;
import com.realestate.emi.security.CustomPrincipal;
import com.realestate.emi.security.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AttendanceService {

    private static final LocalTime LATE_THRESHOLD = LocalTime.of(9, 15);
    private static final LocalTime SHIFT_START = LocalTime.of(9, 0);
    private static final LocalTime EARLY_LEAVE_THRESHOLD = LocalTime.of(17, 45);

    private final AttendanceRepository attendanceRepository;
    private final StaffRepository staffRepository;
    private final UserRepository userRepository;
    private final TenantContext tenantContext;
    private final OrganizationRepository organizationRepository;
    private final HolidayRepository holidayRepository;

    @Transactional
    public AttendanceResponse markAttendance(AttendanceRequest request) {
        Staff staff = staffRepository.findById(request.getStaffId())
                .orElseThrow(() -> new ResourceNotFoundException("Staff", request.getStaffId()));

        if (!staff.getIsActive()) {
            throw new ServiceException("Staff member is not active", "STAFF_INACTIVE");
        }

        // Enforce attendance marking rules
        validateAttendancePermission(staff);

        attendanceRepository.findByStaffIdAndDate(request.getStaffId(), request.getDate())
                .ifPresent(a -> {
                    throw new ServiceException("Attendance already marked for this date", "DUPLICATE_ATTENDANCE");
                });

        Long orgId = tenantContext.getCurrentOrganizationId();
        Organization org = organizationRepository.findById(orgId)
                .orElseThrow(() -> new ServiceException("Organization not found", "ORG_NOT_FOUND"));

        Attendance attendance = Attendance.builder()
                .staff(staff)
                .date(request.getDate())
                .status(request.getStatus())
                .checkIn(request.getCheckIn())
                .checkOut(request.getCheckOut())
                .overtimeHours(request.getOvertimeHours() != null ? request.getOvertimeHours() : 0.0)
                .remarks(request.getRemarks())
                .markedBy(getUsername())
                .build();

        calculateLateStatus(attendance);
        attendance = attendanceRepository.save(attendance);
        log.info("Marked attendance for staff {} on {}: {} (by {})",
                staff.getFullName(), request.getDate(), request.getStatus(), getUsername());
        return toResponse(attendance);
    }

    /**
     * Validates attendance permission:
     * - ADMIN can mark attendance for everyone (including supervisors)
     * - SUPERVISOR can mark attendance for regular staff only (NOT for themselves or other supervisors)
     * - No one can mark their own attendance
     */
    private void validateAttendancePermission(Staff targetStaff) {
        String currentUsername = getUsername();
        Role currentRole = getCurrentUserRole();

        // Check self-marking: if the target staff is linked to the current user
        if (targetStaff.getUser() != null && targetStaff.getUser().getUsername().equals(currentUsername)) {
            throw new ServiceException("You cannot mark your own attendance", "SELF_ATTENDANCE_NOT_ALLOWED");
        }

        // ADMIN can mark for everyone
        if (currentRole == Role.ADMIN) {
            return;
        }

        // SUPERVISOR cannot mark attendance for other supervisors
        if (currentRole == Role.SUPERVISOR) {
            String targetRoleName = targetStaff.getStaffRole().getName().toUpperCase();
            if (targetRoleName.equals("SUPERVISOR")) {
                throw new ServiceException("Supervisors cannot mark attendance for other supervisors. Only Admin can do this.",
                        "INSUFFICIENT_PERMISSION");
            }
        }
    }

    @Transactional
    public AttendanceResponse updateAttendance(Long id, AttendanceRequest request) {
        Attendance attendance = attendanceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Attendance", id));

        attendance.setStatus(request.getStatus());
        attendance.setCheckIn(request.getCheckIn());
        attendance.setCheckOut(request.getCheckOut());
        attendance.setOvertimeHours(request.getOvertimeHours() != null ? request.getOvertimeHours() : 0.0);
        attendance.setRemarks(request.getRemarks());
        attendance.setMarkedBy(getUsername());

        calculateLateStatus(attendance);
        attendance = attendanceRepository.save(attendance);
        log.info("Updated attendance {} for staff {}", id, attendance.getStaff().getFullName());
        return toResponse(attendance);
    }

    @Transactional(readOnly = true)
    public List<AttendanceResponse> getByDate(LocalDate date) {
        return attendanceRepository.findByDateOrderByStaffFullNameAsc(date).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<AttendanceResponse> getByStaffAndDateRange(Long staffId, LocalDate from, LocalDate to) {
        return attendanceRepository.findByStaffIdAndDateBetweenOrderByDateAsc(staffId, from, to).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public AttendanceSummaryResponse getAttendanceSummary(Long staffId, int year, int month) {
        Staff staff = staffRepository.findById(staffId)
                .orElseThrow(() -> new ResourceNotFoundException("Staff", staffId));

        Long orgId = tenantContext.getCurrentOrganizationId();
        YearMonth ym = YearMonth.of(year, month);
        int totalDays = ym.lengthOfMonth();
        LocalDate from = ym.atDay(1);
        LocalDate to = ym.atEndOfMonth();

        // Get holidays for the month
        List<Holiday> holidays = holidayRepository.findByOrganizationIdAndDateBetweenOrderByDateAsc(orgId, from, to);
        Set<LocalDate> mandatoryHolidayDates = holidays.stream()
                .filter(h -> !h.getIsOptional())
                .map(Holiday::getDate)
                .collect(Collectors.toSet());

        // Calculate working days: exclude Sundays and mandatory holidays
        int workingDays = 0;
        for (int d = 1; d <= totalDays; d++) {
            LocalDate date = ym.atDay(d);
            if (date.getDayOfWeek() != DayOfWeek.SUNDAY && !mandatoryHolidayDates.contains(date)) {
                workingDays++;
            }
        }

        // Get attendance records
        List<Attendance> records = attendanceRepository.findByStaffIdAndDateBetweenOrderByDateAsc(staffId, from, to);
        Map<AttendanceStatus, Long> statusCounts = records.stream()
                .collect(Collectors.groupingBy(Attendance::getStatus, Collectors.counting()));

        int presentDays = statusCounts.getOrDefault(AttendanceStatus.PRESENT, 0L).intValue();
        int absentDays = statusCounts.getOrDefault(AttendanceStatus.ABSENT, 0L).intValue();
        int halfDays = statusCounts.getOrDefault(AttendanceStatus.HALF_DAY, 0L).intValue();
        int leaveDays = statusCounts.getOrDefault(AttendanceStatus.LEAVE, 0L).intValue();
        int holidayRecords = statusCounts.getOrDefault(AttendanceStatus.HOLIDAY, 0L).intValue();

        int markedDays = presentDays + absentDays + halfDays + leaveDays + holidayRecords;
        int unmarkedDays = workingDays - markedDays;
        if (unmarkedDays < 0) unmarkedDays = 0;

        double totalOT = records.stream()
                .mapToDouble(a -> a.getOvertimeHours() != null ? a.getOvertimeHours() : 0.0)
                .sum();

        int lateCount = (int) records.stream()
                .filter(a -> "LATE".equals(a.getLateStatus()))
                .count();

        int earlyLeaveCount = (int) records.stream()
                .filter(a -> "EARLY_LEAVE".equals(a.getLateStatus()))
                .count();

        double attendancePercent = workingDays > 0
                ? ((presentDays + halfDays * 0.5) / workingDays) * 100.0
                : 0.0;
        attendancePercent = Math.round(attendancePercent * 100.0) / 100.0;

        return AttendanceSummaryResponse.builder()
                .staffId(staff.getId())
                .staffName(staff.getFullName())
                .staffRole(staff.getStaffRole().getName())
                .year(year)
                .month(month)
                .totalDays(totalDays)
                .workingDays(workingDays)
                .presentDays(presentDays)
                .absentDays(absentDays)
                .halfDays(halfDays)
                .leaveDays(leaveDays)
                .holidays(mandatoryHolidayDates.size() + holidayRecords)
                .unmarkedDays(unmarkedDays)
                .totalOvertimeHours(Math.round(totalOT * 100.0) / 100.0)
                .lateCount(lateCount)
                .earlyLeaveCount(earlyLeaveCount)
                .attendancePercentage(attendancePercent)
                .build();
    }

    @Transactional(readOnly = true)
    public List<AttendanceSummaryResponse> getMonthlyAttendanceSummary(int year, int month) {
        Long orgId = tenantContext.getCurrentOrganizationId();
        List<Staff> activeStaff = staffRepository.findByOrganizationIdAndIsActiveTrueOrderByFullNameAsc(orgId);
        List<AttendanceSummaryResponse> summaries = new ArrayList<>();
        for (Staff staff : activeStaff) {
            summaries.add(getAttendanceSummary(staff.getId(), year, month));
        }
        return summaries;
    }

    private void calculateLateStatus(Attendance attendance) {
        if (attendance.getStatus() != AttendanceStatus.PRESENT
                && attendance.getStatus() != AttendanceStatus.HALF_DAY) {
            attendance.setLateStatus(null);
            attendance.setLateMinutes(null);
            return;
        }

        LocalTime checkIn = attendance.getCheckIn();
        LocalTime checkOut = attendance.getCheckOut();

        if (checkIn != null && checkIn.isAfter(LATE_THRESHOLD)) {
            attendance.setLateStatus("LATE");
            long minutes = ChronoUnit.MINUTES.between(SHIFT_START, checkIn);
            attendance.setLateMinutes(BigDecimal.valueOf(minutes));
        } else if (checkOut != null && checkOut.isBefore(EARLY_LEAVE_THRESHOLD)) {
            attendance.setLateStatus("EARLY_LEAVE");
            attendance.setLateMinutes(null);
        } else {
            attendance.setLateStatus("ON_TIME");
            attendance.setLateMinutes(null);
        }
    }

    private AttendanceResponse toResponse(Attendance attendance) {
        return AttendanceResponse.builder()
                .id(attendance.getId())
                .staffId(attendance.getStaff().getId())
                .staffName(attendance.getStaff().getFullName())
                .staffRole(attendance.getStaff().getStaffRole().getName())
                .date(attendance.getDate())
                .status(attendance.getStatus())
                .checkIn(attendance.getCheckIn())
                .checkOut(attendance.getCheckOut())
                .overtimeHours(attendance.getOvertimeHours())
                .remarks(attendance.getRemarks())
                .markedBy(attendance.getMarkedBy())
                .lateStatus(attendance.getLateStatus())
                .lateMinutes(attendance.getLateMinutes())
                .build();
    }

    private String getUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof CustomPrincipal principal) {
            return principal.getEmail();
        }
        if (authentication != null) {
            return authentication.getName();
        }
        return "unknown";
    }

    private Role getCurrentUserRole() {
        String username = getUsername();
        return userRepository.findByUsername(username)
                .map(User::getRole)
                .orElse(Role.VIEWER);
    }
}
