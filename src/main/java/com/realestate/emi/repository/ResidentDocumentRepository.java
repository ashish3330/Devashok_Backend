package com.realestate.emi.repository;

import com.realestate.emi.entity.ResidentDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ResidentDocumentRepository extends JpaRepository<ResidentDocument, Long> {

    List<ResidentDocument> findByOrganizationIdOrderByCreatedAtDesc(Long orgId);

    List<ResidentDocument> findByOrganizationIdAndVisibleToResidentsTrueOrderByCreatedAtDesc(Long orgId);

    Optional<ResidentDocument> findByIdAndOrganizationId(Long id, Long orgId);
}
