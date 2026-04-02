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

    @Query("SELECT COALESCE(SUM(e.dueAmount - e.paidAmount), 0) FROM EmiSchedule e WHERE e.status IN ('PENDING', 'PARTIAL')")
    BigDecimal sumTotalOutstanding();

    // --- Monthly analytics ---
    @Query("SELECT COUNT(e) FROM EmiSchedule e WHERE e.status = :status AND YEAR(e.dueDate) = :year AND MONTH(e.dueDate) = :month")
    long countByStatusAndMonth(@Param("status") EmiStatus status, @Param("year") int year, @Param("month") int month);

    @Query("SELECT COALESCE(SUM(e.dueAmount - e.paidAmount), 0) FROM EmiSchedule e WHERE e.status IN ('PENDING', 'PARTIAL') AND YEAR(e.dueDate) = :year AND MONTH(e.dueDate) = :month")
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
}
