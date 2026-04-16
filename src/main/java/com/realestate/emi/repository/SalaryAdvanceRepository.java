package com.realestate.emi.repository;

import com.realestate.emi.entity.SalaryAdvance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface SalaryAdvanceRepository extends JpaRepository<SalaryAdvance, Long> {

    List<SalaryAdvance> findByStaffIdAndStatusOrderByAdvanceDateDesc(Long staffId, String status);

    List<SalaryAdvance> findByOrganizationIdAndStatusOrderByAdvanceDateDesc(Long orgId, String status);

    List<SalaryAdvance> findByStaffIdAndOrganizationIdOrderByAdvanceDateDesc(Long staffId, Long orgId);

    @Query("SELECT COALESCE(SUM(sa.balanceRemaining), 0) FROM SalaryAdvance sa WHERE sa.staff.id = :staffId AND sa.status = 'ACTIVE' AND sa.organization.id = :orgId")
    BigDecimal sumActiveBalanceByStaff(@Param("staffId") Long staffId, @Param("orgId") Long orgId);

    @Query("SELECT COUNT(sa) FROM SalaryAdvance sa WHERE sa.status = 'ACTIVE' AND sa.organization.id = :orgId")
    int countActiveByOrg(@Param("orgId") Long orgId);

    @Query("SELECT COALESCE(SUM(sa.balanceRemaining), 0) FROM SalaryAdvance sa WHERE sa.status = 'ACTIVE' AND sa.organization.id = :orgId")
    BigDecimal sumActiveBalanceByOrg(@Param("orgId") Long orgId);

    @Query("SELECT COALESCE(SUM(sa.amount), 0) FROM SalaryAdvance sa WHERE sa.organization.id = :orgId AND YEAR(sa.advanceDate) = :year AND MONTH(sa.advanceDate) = :month")
    BigDecimal sumAdvancesGivenByMonthAndOrg(@Param("year") int year, @Param("month") int month, @Param("orgId") Long orgId);
}
