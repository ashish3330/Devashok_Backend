package com.realestate.emi.repository;

import com.realestate.emi.entity.StaffRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StaffRoleRepository extends JpaRepository<StaffRole, Long> {

    List<StaffRole> findByIsActiveTrueOrderByNameAsc();

    List<StaffRole> findByOrganizationIdAndIsActiveTrueOrderByNameAsc(Long orgId);

    Optional<StaffRole> findByNameIgnoreCase(String name);

    Optional<StaffRole> findByNameIgnoreCaseAndOrganizationId(String name, Long organizationId);
}
