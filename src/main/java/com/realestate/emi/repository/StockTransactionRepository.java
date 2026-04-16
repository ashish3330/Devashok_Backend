package com.realestate.emi.repository;

import com.realestate.emi.entity.StockTransaction;
import com.realestate.emi.enums.TransactionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
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

    @Query("SELECT COALESCE(SUM(st.totalCost), 0) FROM StockTransaction st WHERE st.type = 'INWARD' AND YEAR(st.transactionDate) = :year AND MONTH(st.transactionDate) = :month AND st.material.organization.id = :orgId")
    BigDecimal sumInwardCostByMonthAndOrg(@Param("year") int year, @Param("month") int month, @Param("orgId") Long orgId);

    @Query("SELECT COALESCE(SUM(st.totalCost), 0) FROM StockTransaction st WHERE st.type = 'INWARD' AND st.material.organization.id = :orgId")
    BigDecimal sumTotalInwardCostByOrg(@Param("orgId") Long orgId);

    @Query("SELECT COALESCE(SUM(st.totalCost), 0) FROM StockTransaction st WHERE st.type = :type AND YEAR(st.transactionDate) = :year AND MONTH(st.transactionDate) = :month AND st.material.organization.id = :orgId")
    BigDecimal sumCostByTypeAndMonthAndOrg(@Param("type") TransactionType type, @Param("year") int year, @Param("month") int month, @Param("orgId") Long orgId);

    @Query("SELECT st.material.category, COALESCE(SUM(st.totalCost), 0) FROM StockTransaction st WHERE st.type = 'INWARD' AND YEAR(st.transactionDate) = :year AND MONTH(st.transactionDate) = :month AND st.material.organization.id = :orgId GROUP BY st.material.category ORDER BY SUM(st.totalCost) DESC")
    List<Object[]> sumInwardCostByCategoryAndMonthAndOrg(@Param("year") int year, @Param("month") int month, @Param("orgId") Long orgId);

    @Query("SELECT st.material.name, COALESCE(SUM(st.totalCost), 0) FROM StockTransaction st WHERE st.type = 'INWARD' AND YEAR(st.transactionDate) = :year AND MONTH(st.transactionDate) = :month AND st.material.organization.id = :orgId GROUP BY st.material.id, st.material.name ORDER BY SUM(st.totalCost) DESC")
    List<Object[]> findTopMaterialsBySpendAndMonthAndOrg(@Param("year") int year, @Param("month") int month, @Param("orgId") Long orgId);

    List<StockTransaction> findByMaterialIdAndTypeAndTransactionDateBetween(
            Long materialId, TransactionType type, LocalDateTime from, LocalDateTime to);

    List<StockTransaction> findByMaterialIdAndTransactionDateBetween(
            Long materialId, LocalDateTime from, LocalDateTime to);

    @Query("SELECT st FROM StockTransaction st WHERE st.type IN :types AND st.transactionDate BETWEEN :from AND :to AND st.material.organization.id = :orgId ORDER BY st.transactionDate DESC")
    List<StockTransaction> findByTypesAndDateRangeAndOrg(
            @Param("types") List<TransactionType> types,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            @Param("orgId") Long orgId);

    @Query("SELECT st FROM StockTransaction st WHERE st.material.id = :materialId AND st.transactionDate < :before ORDER BY st.transactionDate DESC")
    List<StockTransaction> findByMaterialIdAndTransactionDateBefore(
            @Param("materialId") Long materialId, @Param("before") LocalDateTime before);

    @Query("SELECT MAX(st.transactionDate) FROM StockTransaction st WHERE st.material.id = :materialId AND st.type = 'INWARD'")
    LocalDateTime findLastInwardDateByMaterialId(@Param("materialId") Long materialId);
}
