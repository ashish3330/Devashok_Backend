package com.realestate.emi.repository;

import com.realestate.emi.entity.Deal;
import com.realestate.emi.entity.EmiSchedule;
import com.realestate.emi.enums.EmiStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface EmiScheduleRepository extends JpaRepository<EmiSchedule, Long> {

    List<EmiSchedule> findByDealOrderByDueDateAsc(Deal deal);

    List<EmiSchedule> findByDealAndStatusInOrderByDueDateAsc(Deal deal, List<EmiStatus> statuses);

    Optional<EmiSchedule> findByIdAndDeal(Long id, Deal deal);

    @Query("SELECT COALESCE(SUM(e.dueAmount + COALESCE(e.bounceCharges, 0) - e.paidAmount), 0) FROM EmiSchedule e WHERE e.status IN ('PENDING', 'PARTIAL')")
    BigDecimal sumTotalOutstanding();

    // --- Monthly analytics ---
    @Query("SELECT COUNT(e) FROM EmiSchedule e WHERE e.status = :status AND YEAR(e.dueDate) = :year AND MONTH(e.dueDate) = :month")
    long countByStatusAndMonth(@Param("status") EmiStatus status, @Param("year") int year, @Param("month") int month);

    @Query("SELECT COALESCE(SUM(e.dueAmount + COALESCE(e.bounceCharges, 0) - e.paidAmount), 0) FROM EmiSchedule e WHERE e.status IN ('PENDING', 'PARTIAL') AND YEAR(e.dueDate) = :year AND MONTH(e.dueDate) = :month")
    BigDecimal sumOutstandingByMonth(@Param("year") int year, @Param("month") int month);

    // --- Upcoming EMIs ---
    @Query("""
        SELECT e FROM EmiSchedule e
        JOIN FETCH e.deal d
        JOIN FETCH d.customer c
        JOIN FETCH d.propertyType pt
        WHERE e.dueDate BETWEEN :startDate AND :endDate
        AND e.status IN ('PENDING', 'PARTIAL')
        ORDER BY e.dueDate ASC
        """)
    List<EmiSchedule> findUpcomingEmis(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    // --- Overdue EMIs in a date range ---
    @Query("""
        SELECT e FROM EmiSchedule e
        JOIN FETCH e.deal d
        JOIN FETCH d.customer c
        JOIN FETCH d.propertyType pt
        WHERE e.dueDate BETWEEN :startDate AND :endDate
        AND e.status IN ('PENDING', 'PARTIAL')
        ORDER BY e.dueDate ASC
        """)
    List<EmiSchedule> findOverdueInRange(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    // All overdue (dueDate < today)
    @Query("""
        SELECT e FROM EmiSchedule e
        JOIN FETCH e.deal d
        JOIN FETCH d.customer c
        JOIN FETCH d.propertyType pt
        WHERE e.dueDate < :today
        AND e.status IN ('PENDING', 'PARTIAL')
        ORDER BY e.dueDate ASC
        """)
    List<EmiSchedule> findAllOverdue(@Param("today") LocalDate today);

    // NPA: distinct deals with EMIs overdue > 90 days
    @Query("SELECT COUNT(DISTINCT e.deal.id) FROM EmiSchedule e WHERE e.dueDate < :cutoff AND e.status IN ('PENDING', 'PARTIAL')")
    long countNpaDeals(@Param("cutoff") LocalDate cutoff);

    // Overdue EMIs not yet bounced — used by bounce charge scheduler
    @Query("SELECT e FROM EmiSchedule e WHERE e.dueDate < :today AND e.status IN ('PENDING', 'PARTIAL') AND e.bounced = false")
    List<EmiSchedule> findOverdueUnbouncedEmis(@Param("today") LocalDate today);

    @Query("SELECT COALESCE(SUM(es.dueAmount - es.paidAmount), 0) FROM EmiSchedule es WHERE es.deal.organization.id = :orgId AND es.status IN ('PENDING','PARTIAL')")
    BigDecimal sumTotalOutstandingByOrg(@Param("orgId") Long orgId);

    @Query("SELECT COUNT(es) FROM EmiSchedule es WHERE es.status = :status AND YEAR(es.dueDate) = :year AND MONTH(es.dueDate) = :month AND es.deal.organization.id = :orgId")
    long countByStatusAndMonthAndOrg(@Param("status") EmiStatus status, @Param("year") int year, @Param("month") int month, @Param("orgId") Long orgId);

    @Query("SELECT COALESCE(SUM(es.dueAmount - es.paidAmount), 0) FROM EmiSchedule es WHERE YEAR(es.dueDate) = :year AND MONTH(es.dueDate) = :month AND es.status IN ('PENDING','PARTIAL') AND es.deal.organization.id = :orgId")
    BigDecimal sumOutstandingByMonthAndOrg(@Param("year") int year, @Param("month") int month, @Param("orgId") Long orgId);

    @Query("SELECT COUNT(DISTINCT es.deal.id) FROM EmiSchedule es WHERE es.dueDate < :cutoffDate AND es.status IN ('PENDING','PARTIAL') AND es.deal.organization.id = :orgId")
    long countNpaDealsByOrg(@Param("cutoffDate") java.time.LocalDate cutoffDate, @Param("orgId") Long orgId);

    @Query("SELECT es FROM EmiSchedule es JOIN FETCH es.deal d JOIN FETCH d.customer JOIN FETCH d.propertyType WHERE es.dueDate BETWEEN :from AND :to AND es.status IN ('PENDING','PARTIAL') AND es.deal.organization.id = :orgId ORDER BY es.dueDate ASC")
    List<com.realestate.emi.entity.EmiSchedule> findUpcomingEmisByOrg(@Param("from") java.time.LocalDate from, @Param("to") java.time.LocalDate to, @Param("orgId") Long orgId);

    @Query("SELECT es FROM EmiSchedule es JOIN FETCH es.deal d JOIN FETCH d.customer JOIN FETCH d.propertyType WHERE es.dueDate < :today AND es.status IN ('PENDING','PARTIAL') AND es.deal.organization.id = :orgId ORDER BY es.dueDate ASC")
    List<com.realestate.emi.entity.EmiSchedule> findAllOverdueByOrg(@Param("today") java.time.LocalDate today, @Param("orgId") Long orgId);
}
