package com.realestate.emi.repository;

import com.realestate.emi.entity.Deal;
import com.realestate.emi.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    List<Payment> findByDealOrderByPaymentDateAsc(Deal deal);

    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p WHERE p.deal.id = :dealId AND p.paymentMethod <> 'INITIAL_DEPOSIT'")
    BigDecimal sumPaymentsByDealId(@Param("dealId") Long dealId);

    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p WHERE p.paymentMethod <> 'INITIAL_DEPOSIT'")
    BigDecimal sumAllPayments();

    @Query("SELECT COUNT(p) FROM Payment p WHERE p.paymentMethod <> 'INITIAL_DEPOSIT' AND YEAR(p.paymentDate) = :year AND MONTH(p.paymentDate) = :month")
    long countByMonth(@Param("year") int year, @Param("month") int month);

    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p WHERE p.paymentMethod <> 'INITIAL_DEPOSIT' AND YEAR(p.paymentDate) = :year AND MONTH(p.paymentDate) = :month")
    BigDecimal sumByMonth(@Param("year") int year, @Param("month") int month);

    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p WHERE p.deal.organization.id = :orgId AND p.paymentMethod != 'INITIAL_DEPOSIT'")
    BigDecimal sumAllPaymentsByOrg(@Param("orgId") Long orgId);

    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p WHERE p.deal.organization.id = :orgId AND p.paymentMethod != 'INITIAL_DEPOSIT' AND YEAR(p.paymentDate) = :year AND MONTH(p.paymentDate) = :month")
    BigDecimal sumByMonthAndOrg(@Param("year") int year, @Param("month") int month, @Param("orgId") Long orgId);
}
