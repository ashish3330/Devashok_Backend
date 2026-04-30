package com.realestate.emi.repository;

import com.realestate.emi.entity.Visitor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VisitorRepository extends JpaRepository<Visitor, Long>, JpaSpecificationExecutor<Visitor> {

    List<Visitor> findByOrganizationIdAndFlatIdOrderByExpectedAtDesc(Long orgId, Long flatId);

    Optional<Visitor> findByIdAndOrganizationId(Long id, Long orgId);

    Optional<Visitor> findByIdAndOrganizationIdAndFlatId(Long id, Long orgId, Long flatId);
}
