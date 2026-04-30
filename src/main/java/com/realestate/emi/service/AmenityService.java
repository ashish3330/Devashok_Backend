package com.realestate.emi.service;

import com.realestate.emi.dto.request.AmenityBookingRequest;
import com.realestate.emi.dto.request.AmenityRequest;
import com.realestate.emi.dto.response.AmenityBookingResponse;
import com.realestate.emi.dto.response.AmenityResponse;
import com.realestate.emi.entity.*;
import com.realestate.emi.enums.AmenityBookingStatus;
import com.realestate.emi.exception.ResourceNotFoundException;
import com.realestate.emi.exception.ServiceException;
import com.realestate.emi.mapper.AmenityMapper;
import com.realestate.emi.repository.*;
import com.realestate.emi.security.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AmenityService {

    private final AmenityRepository amenityRepository;
    private final AmenityBookingRepository bookingRepository;
    private final FlatRepository flatRepository;
    private final ResidentRepository residentRepository;
    private final OrganizationRepository organizationRepository;
    private final AmenityMapper amenityMapper;
    private final TenantContext tenantContext;

    @Transactional(readOnly = true)
    public List<AmenityResponse> findAllAdmin() {
        Long orgId = tenantContext.getCurrentOrganizationId();
        return amenityRepository.findByOrganizationIdOrderByNameAsc(orgId).stream()
                .map(amenityMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<AmenityResponse> findActiveForResident() {
        Long orgId = tenantContext.getCurrentOrganizationId();
        return amenityRepository.findByOrganizationIdAndIsActiveTrueOrderByNameAsc(orgId).stream()
                .map(amenityMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public AmenityResponse findById(Long id) {
        Long orgId = tenantContext.getCurrentOrganizationId();
        Amenity amenity = amenityRepository.findByIdAndOrganizationId(id, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Amenity", id));
        return amenityMapper.toResponse(amenity);
    }

    @Transactional
    public AmenityResponse create(AmenityRequest request) {
        Long orgId = tenantContext.getCurrentOrganizationId();
        Amenity amenity = amenityMapper.toEntity(request);
        Organization org = organizationRepository.findById(orgId)
                .orElseThrow(() -> new ServiceException("Organization not found", "ORG_NOT_FOUND"));
        amenity.setOrganization(org);
        if (request.getIsActive() != null) {
            amenity.setIsActive(request.getIsActive());
        }
        Amenity saved = amenityRepository.save(amenity);
        log.info("Created amenity id={} name={}", saved.getId(), saved.getName());
        return amenityMapper.toResponse(saved);
    }

    @Transactional
    public AmenityResponse update(Long id, AmenityRequest request) {
        Long orgId = tenantContext.getCurrentOrganizationId();
        Amenity existing = amenityRepository.findByIdAndOrganizationId(id, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Amenity", id));
        amenityMapper.updateEntityFromRequest(request, existing);
        if (request.getIsActive() != null) {
            existing.setIsActive(request.getIsActive());
        }
        return amenityMapper.toResponse(amenityRepository.save(existing));
    }

    @Transactional
    public void delete(Long id) {
        Long orgId = tenantContext.getCurrentOrganizationId();
        Amenity existing = amenityRepository.findByIdAndOrganizationId(id, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Amenity", id));
        amenityRepository.delete(existing);
    }

    @Transactional(readOnly = true)
    public List<AmenityBookingResponse> availability(Long amenityId, LocalDate date) {
        Long orgId = tenantContext.getCurrentOrganizationId();
        amenityRepository.findByIdAndOrganizationId(amenityId, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Amenity", amenityId));
        return bookingRepository
                .findByOrganizationIdAndAmenityIdAndBookingDateOrderBySlotStartAsc(orgId, amenityId, date).stream()
                .filter(b -> b.getStatus() != AmenityBookingStatus.CANCELLED)
                .map(amenityMapper::toBookingResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public AmenityBookingResponse residentBook(Long amenityId, AmenityBookingRequest request) {
        Long orgId = tenantContext.getCurrentOrganizationId();
        Long flatId = tenantContext.getCurrentFlatId();
        Long residentId = tenantContext.getCurrentResidentId();
        if (flatId == null || residentId == null) {
            throw new ServiceException("Resident flat context missing", "RESIDENT_CONTEXT_MISSING");
        }
        if (!request.getSlotStart().isBefore(request.getSlotEnd())) {
            throw new ServiceException("slotStart must be before slotEnd", "INVALID_SLOT");
        }
        Organization org = organizationRepository.findById(orgId)
                .orElseThrow(() -> new ServiceException("Organization not found", "ORG_NOT_FOUND"));
        Amenity amenity = amenityRepository.findByIdAndOrganizationId(amenityId, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Amenity", amenityId));
        if (Boolean.FALSE.equals(amenity.getIsActive())) {
            throw new ServiceException("Amenity is not active", "AMENITY_INACTIVE");
        }
        Flat flat = flatRepository.findByIdAndOrganizationId(flatId, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Flat", flatId));
        Resident resident = residentRepository.findByIdAndOrganizationId(residentId, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Resident", residentId));

        // Overlap check against existing non-cancelled bookings on the same amenity/date
        List<AmenityBooking> existing = bookingRepository
                .findByOrganizationIdAndAmenityIdAndBookingDateOrderBySlotStartAsc(orgId, amenityId, request.getBookingDate());
        for (AmenityBooking b : existing) {
            if (b.getStatus() == AmenityBookingStatus.CANCELLED) continue;
            boolean overlap = b.getSlotStart().isBefore(request.getSlotEnd())
                    && request.getSlotStart().isBefore(b.getSlotEnd());
            if (overlap) {
                throw new ServiceException("Slot overlaps an existing booking", "SLOT_OVERLAP");
            }
        }

        BigDecimal price = computePrice(amenity, request);
        AmenityBooking booking = AmenityBooking.builder()
                .organization(org)
                .amenity(amenity)
                .resident(resident)
                .flat(flat)
                .bookingDate(request.getBookingDate())
                .slotStart(request.getSlotStart())
                .slotEnd(request.getSlotEnd())
                .amountPaid(price)
                .paymentStatus(price.compareTo(BigDecimal.ZERO) > 0 ? "PENDING" : "FREE")
                .status(AmenityBookingStatus.BOOKED)
                .notes(request.getNotes())
                .build();
        AmenityBooking saved = bookingRepository.save(booking);
        log.info("Booking created id={} amenity={} flat={}", saved.getId(), amenityId, flatId);
        return amenityMapper.toBookingResponse(saved);
    }

    @Transactional
    public AmenityBookingResponse residentCancel(Long bookingId) {
        Long orgId = tenantContext.getCurrentOrganizationId();
        Long residentId = tenantContext.getCurrentResidentId();
        AmenityBooking booking = bookingRepository.findByIdAndOrganizationIdAndResidentId(bookingId, orgId, residentId)
                .orElseThrow(() -> new ResourceNotFoundException("AmenityBooking", bookingId));
        if (booking.getStatus() != AmenityBookingStatus.BOOKED) {
            throw new ServiceException("Only active bookings can be cancelled", "INVALID_BOOKING_STATE");
        }
        if (booking.getBookingDate().isBefore(LocalDate.now())) {
            throw new ServiceException("Past bookings cannot be cancelled", "BOOKING_IN_PAST");
        }
        booking.setStatus(AmenityBookingStatus.CANCELLED);
        return amenityMapper.toBookingResponse(bookingRepository.save(booking));
    }

    @Transactional(readOnly = true)
    public List<AmenityBookingResponse> myBookings() {
        Long orgId = tenantContext.getCurrentOrganizationId();
        Long residentId = tenantContext.getCurrentResidentId();
        return bookingRepository.findByOrganizationIdAndResidentIdOrderByBookingDateDesc(orgId, residentId).stream()
                .map(amenityMapper::toBookingResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public AmenityBookingResponse adminUpdateStatus(Long bookingId, AmenityBookingStatus status) {
        Long orgId = tenantContext.getCurrentOrganizationId();
        AmenityBooking booking = bookingRepository.findByIdAndOrganizationId(bookingId, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("AmenityBooking", bookingId));
        booking.setStatus(status);
        return amenityMapper.toBookingResponse(bookingRepository.save(booking));
    }

    private BigDecimal computePrice(Amenity amenity, AmenityBookingRequest request) {
        if (amenity.getPricePerSlot() == null || amenity.getSlotDurationMinutes() == null) {
            return BigDecimal.ZERO;
        }
        long minutes = Duration.between(request.getSlotStart(), request.getSlotEnd()).toMinutes();
        long slots = Math.max(1L, minutes / amenity.getSlotDurationMinutes());
        return amenity.getPricePerSlot().multiply(BigDecimal.valueOf(slots));
    }
}
