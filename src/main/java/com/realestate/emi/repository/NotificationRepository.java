package com.realestate.emi.repository;

import com.realestate.emi.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByOrganizationIdAndIsReadFalseOrderByCreatedAtDesc(Long orgId);

    List<Notification> findByOrganizationIdOrderByCreatedAtDesc(Long orgId);

    long countByOrganizationIdAndIsReadFalse(Long orgId);

    long countByOrganizationIdAndIsReadFalseAndSeverity(Long orgId, String severity);

    Optional<Notification> findByNotificationKey(String key);

    boolean existsByNotificationKey(String key);

    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true, n.readAt = CURRENT_TIMESTAMP WHERE n.id = :id AND n.organization.id = :orgId")
    void markAsRead(@Param("id") Long id, @Param("orgId") Long orgId);

    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true, n.readAt = CURRENT_TIMESTAMP WHERE n.organization.id = :orgId AND n.isRead = false")
    void markAllAsRead(@Param("orgId") Long orgId);
}
