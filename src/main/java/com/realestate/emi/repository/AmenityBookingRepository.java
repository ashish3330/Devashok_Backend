package com.realestate.emi.repository;

import com.realestate.emi.entity.AmenityBooking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface AmenityBookingRepository extends JpaRepository<AmenityBooking, Long>, JpaSpecificationExecutor<AmenityBooking> {

    List<AmenityBooking> findByOrganizationIdAndAmenityIdAndBookingDateOrderBySlotStartAsc(Long orgId, Long amenityId, LocalDate bookingDate);

    List<AmenityBooking> findByOrganizationIdAndResidentIdOrderByBookingDateDesc(Long orgId, Long residentId);

    Optional<AmenityBooking> findByIdAndOrganizationId(Long id, Long orgId);

    Optional<AmenityBooking> findByIdAndOrganizationIdAndResidentId(Long id, Long orgId, Long residentId);
}
