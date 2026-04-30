package com.realestate.emi.repository;

import com.realestate.emi.entity.Flat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FlatRepository extends JpaRepository<Flat, Long>, JpaSpecificationExecutor<Flat> {

    List<Flat> findByOrganizationIdOrderByFlatNumberAsc(Long orgId);

    List<Flat> findByOrganizationIdAndBlockIdOrderByFlatNumberAsc(Long orgId, Long blockId);

    Optional<Flat> findByIdAndOrganizationId(Long id, Long orgId);

    boolean existsByOrganizationIdAndBlockIdAndFlatNumber(Long orgId, Long blockId, String flatNumber);

    boolean existsByOrganizationIdAndBlockIdAndFlatNumberAndIdNot(Long orgId, Long blockId, String flatNumber, Long id);
}
