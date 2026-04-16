package com.realestate.emi.repository;

import com.realestate.emi.entity.SupplierPayment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface SupplierPaymentRepository extends JpaRepository<SupplierPayment, Long> {

    List<SupplierPayment> findBySupplierIdAndOrganizationIdOrderByPaymentDateDesc(Long supplierId, Long orgId);

    @Query("SELECT COALESCE(SUM(sp.amount), 0) FROM SupplierPayment sp WHERE sp.supplier.id = :supplierId AND sp.organization.id = :orgId")
    BigDecimal sumTotalPaidBySupplier(@Param("supplierId") Long supplierId, @Param("orgId") Long orgId);

    @Query("SELECT COALESCE(SUM(sp.amount), 0) FROM SupplierPayment sp WHERE sp.organization.id = :orgId AND YEAR(sp.paymentDate) = :year AND MONTH(sp.paymentDate) = :month")
    BigDecimal sumPaymentsByMonthAndOrg(@Param("year") int year, @Param("month") int month, @Param("orgId") Long orgId);

    @Query("SELECT COALESCE(SUM(sp.amount), 0) FROM SupplierPayment sp WHERE sp.organization.id = :orgId")
    BigDecimal sumTotalPaidByOrg(@Param("orgId") Long orgId);

    @Query("SELECT sp.supplier.id, sp.supplier.name, COALESCE(SUM(sp.amount), 0) FROM SupplierPayment sp WHERE sp.organization.id = :orgId AND YEAR(sp.paymentDate) = :year AND MONTH(sp.paymentDate) = :month GROUP BY sp.supplier.id, sp.supplier.name ORDER BY SUM(sp.amount) DESC")
    List<Object[]> findTopSuppliersByPaymentAndMonthAndOrg(@Param("year") int year, @Param("month") int month, @Param("orgId") Long orgId);
}
