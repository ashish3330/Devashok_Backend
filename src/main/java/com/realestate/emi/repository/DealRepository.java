package com.realestate.emi.repository;

import com.realestate.emi.entity.Deal;
import com.realestate.emi.enums.DealStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface DealRepository extends JpaRepository<Deal, Long>, JpaSpecificationExecutor<Deal> {

    @Query("SELECT d FROM Deal d JOIN FETCH d.customer JOIN FETCH d.propertyType ORDER BY d.createdAt DESC")
    List<Deal> findAllWithDetails();

    @Query("SELECT d FROM Deal d JOIN FETCH d.customer JOIN FETCH d.propertyType WHERE d.id = :id")
    Optional<Deal> findByIdWithDetails(@Param("id") Long id);

    @Query("SELECT COALESCE(SUM(d.totalAmount), 0) FROM Deal d")
    BigDecimal sumTotalAmount();

    @Query("SELECT COALESCE(SUM(d.totalPayableAfterDeposit), 0) FROM Deal d")
    BigDecimal sumTotalPayableAfterDeposit();

    long countByStatus(DealStatus status);

    @Query("SELECT d FROM Deal d JOIN FETCH d.customer JOIN FETCH d.propertyType WHERE d.organization.id = :orgId ORDER BY d.createdAt DESC")
    List<Deal> findAllByOrganization(@Param("orgId") Long orgId);

    @Query("SELECT d FROM Deal d JOIN FETCH d.customer JOIN FETCH d.propertyType WHERE d.id = :id AND d.organization.id = :orgId")
    Optional<Deal> findByIdAndOrganization(@Param("id") Long id, @Param("orgId") Long orgId);

    long countByStatusAndOrganizationId(DealStatus status, Long orgId);

    boolean existsByPropertyTypeId(Long propertyTypeId);

    @Query("SELECT AVG(d.totalPayableAfterDeposit) FROM Deal d WHERE d.totalPayableAfterDeposit IS NOT NULL")
    BigDecimal avgDealValue();

    @Query("SELECT AVG(d.emiAmountPerMonth) FROM Deal d WHERE d.emiAmountPerMonth IS NOT NULL")
    BigDecimal avgEmiAmount();

    @Query("SELECT d.propertyType.name, COUNT(d), COALESCE(SUM(d.totalPayableAfterDeposit), 0) FROM Deal d GROUP BY d.propertyType.name ORDER BY COUNT(d) DESC")
    List<Object[]> distributionByPropertyType();

    @Query("SELECT COALESCE(SUM(d.totalPayableAfterDeposit), 0) FROM Deal d WHERE d.organization.id = :orgId")
    BigDecimal sumTotalPayableAfterDepositByOrg(@Param("orgId") Long orgId);

    @Query("SELECT AVG(d.totalPayableAfterDeposit) FROM Deal d WHERE d.totalPayableAfterDeposit IS NOT NULL AND d.organization.id = :orgId")
    BigDecimal avgDealValueByOrg(@Param("orgId") Long orgId);

    @Query("SELECT AVG(d.emiAmountPerMonth) FROM Deal d WHERE d.emiAmountPerMonth IS NOT NULL AND d.organization.id = :orgId")
    BigDecimal avgEmiAmountByOrg(@Param("orgId") Long orgId);

    @Query("SELECT COALESCE(SUM(d.totalAmount), 0) FROM Deal d WHERE d.organization.id = :orgId")
    BigDecimal sumTotalAmountByOrg(@Param("orgId") Long orgId);

    @Query("SELECT d.propertyType.name, COUNT(d), COALESCE(SUM(d.totalPayableAfterDeposit), 0) FROM Deal d WHERE d.organization.id = :orgId GROUP BY d.propertyType.name ORDER BY COUNT(d) DESC")
    List<Object[]> distributionByPropertyTypeAndOrg(@Param("orgId") Long orgId);
}
