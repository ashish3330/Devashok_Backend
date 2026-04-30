package com.realestate.emi.repository;

import com.realestate.emi.entity.Block;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BlockRepository extends JpaRepository<Block, Long>, JpaSpecificationExecutor<Block> {

    List<Block> findByOrganizationIdOrderByNameAsc(Long orgId);

    Optional<Block> findByIdAndOrganizationId(Long id, Long orgId);

    boolean existsByCodeAndOrganizationId(String code, Long orgId);

    boolean existsByCodeAndIdNotAndOrganizationId(String code, Long id, Long orgId);
}
