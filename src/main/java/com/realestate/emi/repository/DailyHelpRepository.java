package com.realestate.emi.repository;

import com.realestate.emi.entity.DailyHelp;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DailyHelpRepository extends JpaRepository<DailyHelp, Long> {

    List<DailyHelp> findByOrganizationIdOrderByNameAsc(Long orgId);

    Optional<DailyHelp> findByIdAndOrganizationId(Long id, Long orgId);

    boolean existsByOrganizationIdAndPrimaryPhone(Long orgId, String primaryPhone);
}
