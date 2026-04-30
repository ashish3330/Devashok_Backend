package com.realestate.emi.repository;

import com.realestate.emi.entity.Resident;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ResidentRepository extends JpaRepository<Resident, Long>, JpaSpecificationExecutor<Resident> {

    List<Resident> findByOrganizationIdOrderByFullNameAsc(Long orgId);

    List<Resident> findByOrganizationIdAndFlatIdOrderByFullNameAsc(Long orgId, Long flatId);

    Optional<Resident> findByIdAndOrganizationId(Long id, Long orgId);
}
