package com.realestate.emi.service;

import com.realestate.emi.dto.request.AttendanceRequest;
import com.realestate.emi.dto.response.AttendanceResponse;
import com.realestate.emi.entity.Attendance;
import com.realestate.emi.entity.Organization;
import com.realestate.emi.entity.Staff;
import com.realestate.emi.entity.User;
import com.realestate.emi.enums.Role;
import com.realestate.emi.exception.ResourceNotFoundException;
import com.realestate.emi.exception.ServiceException;
import com.realestate.emi.repository.AttendanceRepository;
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

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AttendanceService {

    private final AttendanceRepository attendanceRepository;
    private final StaffRepository staffRepository;
    private final UserRepository userRepository;
    private final TenantContext tenantContext;
    private final OrganizationRepository organizationRepository;

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
