package com.realestate.emi.repository;

import com.realestate.emi.entity.MaintenanceBill;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MaintenanceBillRepository extends JpaRepository<MaintenanceBill, Long>, JpaSpecificationExecutor<MaintenanceBill> {

    List<MaintenanceBill> findByOrganizationIdAndFlatIdOrderByBillMonthDesc(Long orgId, Long flatId);

    Optional<MaintenanceBill> findByIdAndOrganizationId(Long id, Long orgId);

    Optional<MaintenanceBill> findByOrganizationIdAndFlatIdAndBillMonth(Long orgId, Long flatId, String billMonth);
}
