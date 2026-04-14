package com.realestate.emi.repository;

import com.realestate.emi.entity.Deal;
import com.realestate.emi.entity.InstallmentPhase;
import com.realestate.emi.enums.PhaseStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface InstallmentPhaseRepository extends JpaRepository<InstallmentPhase, Long> {

    List<InstallmentPhase> findByDealOrderByPhaseOrderAsc(Deal deal);

    List<InstallmentPhase> findByDealAndStatusInOrderByPhaseOrderAsc(Deal deal, List<PhaseStatus> statuses);

    Optional<InstallmentPhase> findByIdAndDeal(Long id, Deal deal);

    @Query("SELECT COALESCE(SUM(ip.dueAmount - ip.paidAmount), 0) FROM InstallmentPhase ip WHERE ip.status IN :statuses")
    BigDecimal sumOutstandingByStatuses(@Param("statuses") List<PhaseStatus> statuses);

    @Query("SELECT ip FROM InstallmentPhase ip WHERE ip.status IN ('DUE','PARTIAL') AND ip.dueDeadline < :today")
    List<InstallmentPhase> findOverduePhases(@Param("today") LocalDate today);

    @Query("SELECT ip FROM InstallmentPhase ip JOIN FETCH ip.deal d JOIN FETCH d.customer WHERE ip.status IN ('DUE','OVERDUE','PARTIAL') AND ip.dueDeadline IS NOT NULL AND ip.dueDeadline <= :maxDate AND d.organization.id = :orgId ORDER BY ip.dueDeadline ASC")
    List<InstallmentPhase> findUpcomingDuePhasesByOrg(@Param("maxDate") LocalDate maxDate, @Param("orgId") Long orgId);
}
