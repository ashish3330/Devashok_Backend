package com.realestate.emi.service;

import com.realestate.emi.dto.request.DailyHelpAssignmentRequest;
import com.realestate.emi.dto.request.DailyHelpRequest;
import com.realestate.emi.dto.response.DailyHelpAssignmentResponse;
import com.realestate.emi.dto.response.DailyHelpAttendanceResponse;
import com.realestate.emi.dto.response.DailyHelpResponse;
import com.realestate.emi.entity.*;
import com.realestate.emi.exception.ResourceNotFoundException;
import com.realestate.emi.exception.ServiceException;
import com.realestate.emi.mapper.DailyHelpMapper;
import com.realestate.emi.repository.*;
import com.realestate.emi.security.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DailyHelpService {

    private final DailyHelpRepository helpRepository;
    private final DailyHelpAssignmentRepository assignmentRepository;
    private final DailyHelpAttendanceRepository attendanceRepository;
    private final FlatRepository flatRepository;
    private final ResidentRepository residentRepository;
    private final OrganizationRepository organizationRepository;
    private final DailyHelpMapper helpMapper;
    private final TenantContext tenantContext;

    // ----- Admin: directory CRUD -----

    @Transactional(readOnly = true)
    public List<DailyHelpResponse> findAll() {
        Long orgId = tenantContext.getCurrentOrganizationId();
        return helpRepository.findByOrganizationIdOrderByNameAsc(orgId).stream()
                .map(helpMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public DailyHelpResponse findById(Long id) {
        Long orgId = tenantContext.getCurrentOrganizationId();
        DailyHelp help = helpRepository.findByIdAndOrganizationId(id, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("DailyHelp", id));
        return helpMapper.toResponse(help);
    }

    @Transactional
    public DailyHelpResponse create(DailyHelpRequest request) {
        Long orgId = tenantContext.getCurrentOrganizationId();
        if (helpRepository.existsByOrganizationIdAndPrimaryPhone(orgId, request.getPrimaryPhone())) {
            throw new ServiceException("Help with this phone already exists", "DUPLICATE_PHONE");
        }
        DailyHelp help = helpMapper.toEntity(request);
        Organization org = organizationRepository.findById(orgId)
                .orElseThrow(() -> new ServiceException("Organization not found", "ORG_NOT_FOUND"));
        help.setOrganization(org);
        if (request.getIsVerified() != null) {
            help.setIsVerified(request.getIsVerified());
        }
        DailyHelp saved = helpRepository.save(help);
        log.info("Created daily help id={} name={}", saved.getId(), saved.getName());
        return helpMapper.toResponse(saved);
    }

    @Transactional
    public DailyHelpResponse update(Long id, DailyHelpRequest request) {
        Long orgId = tenantContext.getCurrentOrganizationId();
        DailyHelp existing = helpRepository.findByIdAndOrganizationId(id, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("DailyHelp", id));
        helpMapper.updateEntityFromRequest(request, existing);
        if (request.getIsVerified() != null) {
            existing.setIsVerified(request.getIsVerified());
        }
        return helpMapper.toResponse(helpRepository.save(existing));
    }

    @Transactional
    public void delete(Long id) {
        Long orgId = tenantContext.getCurrentOrganizationId();
        DailyHelp existing = helpRepository.findByIdAndOrganizationId(id, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("DailyHelp", id));
        helpRepository.delete(existing);
    }

    // ----- Admin: attendance gate ops -----

    @Transactional
    public DailyHelpAttendanceResponse adminCheckIn(Long helpId, Long flatId) {
        Long orgId = tenantContext.getCurrentOrganizationId();
        Organization org = organizationRepository.findById(orgId)
                .orElseThrow(() -> new ServiceException("Organization not found", "ORG_NOT_FOUND"));
        DailyHelp help = helpRepository.findByIdAndOrganizationId(helpId, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("DailyHelp", helpId));
        Flat flat = flatRepository.findByIdAndOrganizationId(flatId, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Flat", flatId));
        DailyHelpAttendance att = DailyHelpAttendance.builder()
                .organization(org)
                .help(help)
                .flat(flat)
                .checkedInAt(LocalDateTime.now())
                .build();
        return helpMapper.toAttendanceResponse(attendanceRepository.save(att));
    }

    @Transactional
    public DailyHelpAttendanceResponse adminCheckOut(Long attendanceId) {
        Long orgId = tenantContext.getCurrentOrganizationId();
        DailyHelpAttendance att = attendanceRepository.findById(attendanceId)
                .filter(a -> a.getOrganization().getId().equals(orgId))
                .orElseThrow(() -> new ResourceNotFoundException("Attendance", attendanceId));
        att.setCheckedOutAt(LocalDateTime.now());
        return helpMapper.toAttendanceResponse(attendanceRepository.save(att));
    }

    @Transactional(readOnly = true)
    public List<DailyHelpAttendanceResponse> adminAttendance(Long helpId, LocalDate from, LocalDate to) {
        Long orgId = tenantContext.getCurrentOrganizationId();
        if (helpId == null) {
            throw new ServiceException("helpId is required", "HELP_ID_REQUIRED");
        }
        LocalDateTime fromTs = from != null ? from.atStartOfDay() : LocalDate.now().minusDays(30).atStartOfDay();
        LocalDateTime toTs = to != null ? to.plusDays(1).atStartOfDay() : LocalDate.now().plusDays(1).atStartOfDay();
        return attendanceRepository.findByOrganizationIdAndHelpIdAndCheckedInAtBetween(orgId, helpId, fromTs, toTs).stream()
                .sorted((a, b) -> b.getCheckedInAt().compareTo(a.getCheckedInAt()))
                .map(helpMapper::toAttendanceResponse)
                .collect(Collectors.toList());
    }

    // ----- Resident: assignments + attendance -----

    @Transactional(readOnly = true)
    public List<DailyHelpAssignmentResponse> residentAssignments() {
        Long orgId = tenantContext.getCurrentOrganizationId();
        Long flatId = tenantContext.getCurrentFlatId();
        if (flatId == null) {
            throw new ServiceException("Resident flat context missing", "RESIDENT_CONTEXT_MISSING");
        }
        return assignmentRepository.findByOrganizationIdAndFlatIdAndIsActiveTrue(orgId, flatId).stream()
                .map(helpMapper::toAssignmentResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public DailyHelpAssignmentResponse residentAssign(DailyHelpAssignmentRequest request) {
        Long orgId = tenantContext.getCurrentOrganizationId();
        Long flatId = tenantContext.getCurrentFlatId();
        Long residentId = tenantContext.getCurrentResidentId();
        if (flatId == null || residentId == null) {
            throw new ServiceException("Resident flat context missing", "RESIDENT_CONTEXT_MISSING");
        }
        if (assignmentRepository.existsByOrganizationIdAndHelpIdAndFlatIdAndIsActiveTrue(orgId, request.getHelpId(), flatId)) {
            throw new ServiceException("Help is already assigned to this flat", "DUPLICATE_ASSIGNMENT");
        }
        Organization org = organizationRepository.findById(orgId)
                .orElseThrow(() -> new ServiceException("Organization not found", "ORG_NOT_FOUND"));
        DailyHelp help = helpRepository.findByIdAndOrganizationId(request.getHelpId(), orgId)
                .orElseThrow(() -> new ResourceNotFoundException("DailyHelp", request.getHelpId()));
        Flat flat = flatRepository.findByIdAndOrganizationId(flatId, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Flat", flatId));
        Resident resident = residentRepository.findByIdAndOrganizationId(residentId, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Resident", residentId));

        DailyHelpAssignment assignment = DailyHelpAssignment.builder()
                .organization(org)
                .help(help)
                .flat(flat)
                .resident(resident)
                .startDate(request.getStartDate() != null ? request.getStartDate() : LocalDate.now())
                .monthlySalary(request.getMonthlySalary())
                .isActive(true)
                .build();
        return helpMapper.toAssignmentResponse(assignmentRepository.save(assignment));
    }

    @Transactional
    public void residentUnassign(Long assignmentId) {
        Long orgId = tenantContext.getCurrentOrganizationId();
        Long flatId = tenantContext.getCurrentFlatId();
        DailyHelpAssignment assignment = assignmentRepository.findByIdAndOrganizationIdAndFlatId(assignmentId, orgId, flatId)
                .orElseThrow(() -> new ResourceNotFoundException("DailyHelpAssignment", assignmentId));
        assignment.setIsActive(false);
        assignment.setEndDate(LocalDate.now());
        assignmentRepository.save(assignment);
    }

    @Transactional(readOnly = true)
    public List<DailyHelpAttendanceResponse> residentAttendance(LocalDate from, LocalDate to) {
        Long orgId = tenantContext.getCurrentOrganizationId();
        Long flatId = tenantContext.getCurrentFlatId();
        if (flatId == null) {
            throw new ServiceException("Resident flat context missing", "RESIDENT_CONTEXT_MISSING");
        }
        LocalDateTime fromTs = from != null ? from.atStartOfDay() : LocalDate.now().minusDays(7).atStartOfDay();
        LocalDateTime toTs = to != null ? to.plusDays(1).atStartOfDay() : LocalDate.now().plusDays(1).atStartOfDay();
        return attendanceRepository.findByOrganizationIdAndFlatIdAndCheckedInAtBetween(orgId, flatId, fromTs, toTs).stream()
                .sorted((a, b) -> b.getCheckedInAt().compareTo(a.getCheckedInAt()))
                .map(helpMapper::toAttendanceResponse)
                .collect(Collectors.toList());
    }
}
