package com.realestate.emi.repository;

import com.realestate.emi.entity.StockTransaction;
import com.realestate.emi.enums.TransactionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface StockTransactionRepository extends JpaRepository<StockTransaction, Long> {

    List<StockTransaction> findByMaterialIdOrderByTransactionDateDesc(Long materialId);

    List<StockTransaction> findByDealIdOrderByTransactionDateDesc(Long dealId);

    @Query("SELECT COALESCE(SUM(st.totalCost), 0) FROM StockTransaction st WHERE st.deal.id = :dealId AND st.type = 'OUTWARD'")
    BigDecimal sumMaterialCostByDealId(@Param("dealId") Long dealId);

    @Query("SELECT COALESCE(SUM(st.totalCost), 0) FROM StockTransaction st WHERE st.deal.id = :dealId AND st.installmentPhase.id = :phaseId AND st.type = 'OUTWARD'")
    BigDecimal sumMaterialCostByDealAndPhase(@Param("dealId") Long dealId, @Param("phaseId") Long phaseId);

    List<StockTransaction> findBySupplierIdOrderByTransactionDateDesc(Long supplierId);

    List<StockTransaction> findByTypeOrderByTransactionDateDesc(TransactionType type);

    @Query("SELECT COALESCE(SUM(st.totalCost), 0) FROM StockTransaction st WHERE st.type = 'OUTWARD' AND YEAR(st.transactionDate) = :year AND MONTH(st.transactionDate) = :month")
    BigDecimal sumOutwardCostByMonth(@Param("year") int year, @Param("month") int month);

    @Query("SELECT COALESCE(SUM(st.totalCost), 0) FROM StockTransaction st WHERE st.type = 'OUTWARD' AND YEAR(st.transactionDate) = :year AND MONTH(st.transactionDate) = :month AND st.material.organization.id = :orgId")
    BigDecimal sumOutwardCostByMonthAndOrg(@Param("year") int year, @Param("month") int month, @Param("orgId") Long orgId);

    @Query("SELECT COALESCE(SUM(st.totalCost), 0) FROM StockTransaction st WHERE st.supplier.id = :supplierId AND st.type = 'INWARD' AND st.material.organization.id = :orgId")
    BigDecimal sumInwardCostBySupplier(@Param("supplierId") Long supplierId, @Param("orgId") Long orgId);
}
