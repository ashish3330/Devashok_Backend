package com.realestate.emi.repository;

import com.realestate.emi.entity.MaintenancePayment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MaintenancePaymentRepository extends JpaRepository<MaintenancePayment, Long> {

    List<MaintenancePayment> findByOrganizationIdAndBillIdOrderByPaymentDateDesc(Long orgId, Long billId);

    List<MaintenancePayment> findByOrganizationIdAndResidentIdOrderByPaymentDateDesc(Long orgId, Long residentId);

    Optional<MaintenancePayment> findByIdAndOrganizationId(Long id, Long orgId);
}
