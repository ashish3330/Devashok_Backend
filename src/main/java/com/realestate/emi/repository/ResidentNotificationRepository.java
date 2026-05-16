package com.realestate.emi.repository;

import com.realestate.emi.entity.ResidentNotification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ResidentNotificationRepository extends JpaRepository<ResidentNotification, Long> {

    List<ResidentNotification> findTop50ByResidentIdAndOrganizationIdOrderByCreatedAtDesc(Long residentId, Long organizationId);

    long countByResidentIdAndOrganizationIdAndReadAtIsNull(Long residentId, Long organizationId);

    Optional<ResidentNotification> findByIdAndResidentIdAndOrganizationId(Long id, Long residentId, Long organizationId);

    @Modifying
    @Query("update ResidentNotification n set n.readAt = :now where n.resident.id = :residentId and n.organization.id = :orgId and n.readAt is null")
    int markAllRead(@Param("residentId") Long residentId, @Param("orgId") Long orgId, @Param("now") LocalDateTime now);
}
