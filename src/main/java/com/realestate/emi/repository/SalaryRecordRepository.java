package com.realestate.emi.repository;

import com.realestate.emi.entity.SalaryRecord;
import com.realestate.emi.enums.SalaryStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface SalaryRecordRepository extends JpaRepository<SalaryRecord, Long> {

    Optional<SalaryRecord> findByStaffIdAndYearAndMonth(Long staffId, Integer year, Integer month);

    List<SalaryRecord> findByYearAndMonthOrderByStaffFullNameAsc(Integer year, Integer month);

    List<SalaryRecord> findByStaffIdOrderByYearDescMonthDesc(Long staffId);

    List<SalaryRecord> findByStatus(SalaryStatus status);

    @Query("SELECT COALESCE(SUM(sr.netSalary), 0) FROM SalaryRecord sr WHERE sr.year = :year AND sr.month = :month")
    BigDecimal sumNetSalaryByMonth(@Param("year") int year, @Param("month") int month);

    @Query("SELECT COALESCE(SUM(sr.amountPaid), 0) FROM SalaryRecord sr WHERE sr.year = :year AND sr.month = :month")
    BigDecimal sumPaidByMonth(@Param("year") int year, @Param("month") int month);

    @Query("SELECT COALESCE(SUM(sr.netSalary), 0) FROM SalaryRecord sr WHERE sr.year = :year AND sr.month = :month AND sr.staff.organization.id = :orgId")
    BigDecimal sumNetSalaryByMonthAndOrg(@Param("year") int year, @Param("month") int month, @Param("orgId") Long orgId);

    @Query("SELECT COALESCE(SUM(sr.amountPaid), 0) FROM SalaryRecord sr WHERE sr.year = :year AND sr.month = :month AND sr.staff.organization.id = :orgId")
    BigDecimal sumPaidByMonthAndOrg(@Param("year") int year, @Param("month") int month, @Param("orgId") Long orgId);

    List<SalaryRecord> findByYearAndMonthAndStaffOrganizationIdOrderByStaffFullNameAsc(Integer year, Integer month, Long orgId);

    @Query("SELECT sr FROM SalaryRecord sr WHERE sr.status = :status AND sr.staff.organization.id = :orgId")
    List<SalaryRecord> findByStatusAndOrg(@Param("status") SalaryStatus status, @Param("orgId") Long orgId);

    List<SalaryRecord> findByStaffIdAndStaffOrganizationIdOrderByYearDescMonthDesc(Long staffId, Long orgId);
}
