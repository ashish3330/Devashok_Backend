package com.realestate.emi.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "society_resident_users", uniqueConstraints = {
        @UniqueConstraint(name = "uk_resident_user_resident", columnNames = {"resident_id"}),
        @UniqueConstraint(name = "uk_resident_user_phone", columnNames = {"phone"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResidentUser extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "resident_id", nullable = false)
    private Resident resident;

    @Column(name = "phone", nullable = false, length = 20)
    private String phone;

    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    @Column(name = "fcm_token", length = 500)
    private String fcmToken;

    @Column(name = "device_info", length = 500)
    private String deviceInfo;
}
