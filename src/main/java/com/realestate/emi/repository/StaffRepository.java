package com.realestate.emi.repository;

import com.realestate.emi.entity.Staff;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface StaffRepository extends JpaRepository<Staff, Long> {

    List<Staff> findByIsActiveTrueOrderByFullNameAsc();

    List<Staff> findByOrganizationIdAndIsActiveTrueOrderByFullNameAsc(Long orgId);

    List<Staff> findByStaffRoleIdAndIsActiveTrue(Long roleId);

    List<Staff> findByStaffRoleIdAndIsActiveTrueAndOrganizationId(Long roleId, Long orgId);

    @Query("SELECT COALESCE(SUM(s.monthlySalary), 0) FROM Staff s WHERE s.isActive = true")
    BigDecimal sumTotalMonthlySalary();

    @Query("SELECT COALESCE(SUM(s.monthlySalary), 0) FROM Staff s WHERE s.isActive = true AND s.organization.id = :orgId")
    BigDecimal sumTotalMonthlySalaryByOrg(@org.springframework.data.repository.query.Param("orgId") Long orgId);

    Optional<Staff> findByUserId(Long userId);

    @Query("SELECT s.employeeCode FROM Staff s WHERE s.organization.id = :orgId AND s.employeeCode IS NOT NULL ORDER BY s.employeeCode DESC LIMIT 1")
    String findMaxEmployeeCodeByOrg(@org.springframework.data.repository.query.Param("orgId") Long orgId);

    List<Staff> findByOrganizationIdOrderByFullNameAsc(Long orgId);
}
