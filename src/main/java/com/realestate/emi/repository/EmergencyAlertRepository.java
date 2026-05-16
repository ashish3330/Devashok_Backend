package com.realestate.emi.repository;

import com.realestate.emi.entity.EmergencyAlert;
import com.realestate.emi.enums.EmergencyStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EmergencyAlertRepository extends JpaRepository<EmergencyAlert, Long> {

    List<EmergencyAlert> findAllByOrganizationIdOrderByCreatedAtDesc(Long organizationId);

    List<EmergencyAlert> findAllByOrganizationIdAndStatusOrderByCreatedAtDesc(Long organizationId, EmergencyStatus status);

    List<EmergencyAlert> findAllByResidentIdAndOrganizationIdOrderByCreatedAtDesc(Long residentId, Long organizationId);

    Optional<EmergencyAlert> findByIdAndOrganizationId(Long id, Long organizationId);
}
