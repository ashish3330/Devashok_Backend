package com.realestate.emi.repository;

import com.realestate.emi.entity.ComplaintCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ComplaintCategoryRepository extends JpaRepository<ComplaintCategory, Long> {

    List<ComplaintCategory> findByOrganizationIdOrderByNameAsc(Long orgId);

    Optional<ComplaintCategory> findByIdAndOrganizationId(Long id, Long orgId);

    boolean existsByOrganizationIdAndName(Long orgId, String name);
}
