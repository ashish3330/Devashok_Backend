package com.realestate.emi.repository;

import com.realestate.emi.entity.Notice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface NoticeRepository extends JpaRepository<Notice, Long>, JpaSpecificationExecutor<Notice> {

    List<Notice> findByOrganizationIdOrderByPublishedAtDesc(Long orgId);

    Optional<Notice> findByIdAndOrganizationId(Long id, Long orgId);
}
