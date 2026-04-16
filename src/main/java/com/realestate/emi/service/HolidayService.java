package com.realestate.emi.service;

import com.realestate.emi.dto.request.HolidayRequest;
import com.realestate.emi.dto.response.HolidayResponse;
import com.realestate.emi.entity.Holiday;
import com.realestate.emi.entity.Organization;
import com.realestate.emi.exception.ResourceNotFoundException;
import com.realestate.emi.exception.ServiceException;
import com.realestate.emi.repository.HolidayRepository;
import com.realestate.emi.repository.OrganizationRepository;
import com.realestate.emi.security.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class HolidayService {

    private final HolidayRepository holidayRepository;
    private final OrganizationRepository organizationRepository;
    private final TenantContext tenantContext;

    @Transactional(readOnly = true)
    public List<HolidayResponse> findAll() {
        Long orgId = tenantContext.getCurrentOrganizationId();
        return holidayRepository.findByOrganizationIdOrderByDateAsc(orgId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<HolidayResponse> findByYear(int year) {
        Long orgId = tenantContext.getCurrentOrganizationId();
        LocalDate from = LocalDate.of(year, 1, 1);
        LocalDate to = LocalDate.of(year, 12, 31);
        return holidayRepository.findByOrganizationIdAndDateBetweenOrderByDateAsc(orgId, from, to).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public HolidayResponse create(HolidayRequest request) {
        Long orgId = tenantContext.getCurrentOrganizationId();
        Organization org = organizationRepository.findById(orgId)
                .orElseThrow(() -> new ServiceException("Organization not found", "ORG_NOT_FOUND"));

        holidayRepository.findByDateAndOrganizationId(request.getDate(), orgId)
                .ifPresent(h -> {
                    throw new ServiceException("Holiday already exists for this date: " + h.getName(), "DUPLICATE_HOLIDAY");
                });

        Holiday holiday = Holiday.builder()
                .date(request.getDate())
                .name(request.getName())
                .type(request.getType())
                .isOptional(request.getIsOptional() != null ? request.getIsOptional() : false)
                .organization(org)
                .build();

        holiday = holidayRepository.save(holiday);
        log.info("Created holiday: {} on {} for org {}", holiday.getName(), holiday.getDate(), orgId);
        return toResponse(holiday);
    }

    @Transactional
    public HolidayResponse update(Long id, HolidayRequest request) {
        Long orgId = tenantContext.getCurrentOrganizationId();
        Holiday holiday = holidayRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Holiday", id));

        if (!holiday.getOrganization().getId().equals(orgId)) {
            throw new ResourceNotFoundException("Holiday", id);
        }

        // Check duplicate date if date changed
        if (!holiday.getDate().equals(request.getDate())) {
            holidayRepository.findByDateAndOrganizationId(request.getDate(), orgId)
                    .ifPresent(h -> {
                        throw new ServiceException("Holiday already exists for this date: " + h.getName(), "DUPLICATE_HOLIDAY");
                    });
        }

        holiday.setDate(request.getDate());
        holiday.setName(request.getName());
        holiday.setType(request.getType());
        holiday.setIsOptional(request.getIsOptional() != null ? request.getIsOptional() : false);

        holiday = holidayRepository.save(holiday);
        log.info("Updated holiday {}: {} on {}", id, holiday.getName(), holiday.getDate());
        return toResponse(holiday);
    }

    @Transactional
    public void delete(Long id) {
        Long orgId = tenantContext.getCurrentOrganizationId();
        Holiday holiday = holidayRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Holiday", id));

        if (!holiday.getOrganization().getId().equals(orgId)) {
            throw new ResourceNotFoundException("Holiday", id);
        }

        holidayRepository.delete(holiday);
        log.info("Deleted holiday {}: {} on {}", id, holiday.getName(), holiday.getDate());
    }

    @Transactional(readOnly = true)
    public List<Holiday> getHolidaysBetween(LocalDate from, LocalDate to) {
        Long orgId = tenantContext.getCurrentOrganizationId();
        return holidayRepository.findByOrganizationIdAndDateBetweenOrderByDateAsc(orgId, from, to);
    }

    @Transactional(readOnly = true)
    public boolean isHoliday(LocalDate date) {
        Long orgId = tenantContext.getCurrentOrganizationId();
        return holidayRepository.findByDateAndOrganizationId(date, orgId)
                .map(h -> !h.getIsOptional())
                .orElse(false);
    }

    private HolidayResponse toResponse(Holiday holiday) {
        return HolidayResponse.builder()
                .id(holiday.getId())
                .date(holiday.getDate())
                .name(holiday.getName())
                .type(holiday.getType())
                .isOptional(holiday.getIsOptional())
                .build();
    }
}
