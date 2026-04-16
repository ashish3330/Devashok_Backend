package com.realestate.emi.repository;

import com.realestate.emi.entity.Holiday;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface HolidayRepository extends JpaRepository<Holiday, Long> {

    List<Holiday> findByOrganizationIdOrderByDateAsc(Long orgId);

    List<Holiday> findByOrganizationIdAndDateBetweenOrderByDateAsc(Long orgId, LocalDate from, LocalDate to);

    Optional<Holiday> findByDateAndOrganizationId(LocalDate date, Long orgId);

    long countByOrganizationIdAndDateBetweenAndIsOptionalFalse(Long orgId, LocalDate from, LocalDate to);
}
