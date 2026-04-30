package com.realestate.emi.repository;

import com.realestate.emi.entity.DailyHelpAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DailyHelpAssignmentRepository extends JpaRepository<DailyHelpAssignment, Long> {

    List<DailyHelpAssignment> findByOrganizationIdAndFlatIdAndIsActiveTrue(Long orgId, Long flatId);

    List<DailyHelpAssignment> findByOrganizationIdAndHelpId(Long orgId, Long helpId);

    Optional<DailyHelpAssignment> findByIdAndOrganizationId(Long id, Long orgId);
}
