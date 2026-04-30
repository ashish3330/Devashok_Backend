package com.realestate.emi.repository;

import com.realestate.emi.entity.Complaint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ComplaintRepository extends JpaRepository<Complaint, Long>, JpaSpecificationExecutor<Complaint> {

    List<Complaint> findByOrganizationIdAndFlatIdOrderByCreatedAtDesc(Long orgId, Long flatId);

    Optional<Complaint> findByIdAndOrganizationId(Long id, Long orgId);
}
