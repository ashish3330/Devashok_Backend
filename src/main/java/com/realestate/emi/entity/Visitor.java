package com.realestate.emi.entity;

import com.realestate.emi.enums.VisitorStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "society_visitors", indexes = {
        @Index(name = "idx_visitor_org_flat", columnList = "organization_id, flat_id"),
        @Index(name = "idx_visitor_status", columnList = "status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Visitor extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "flat_id", nullable = false)
    private Flat flat;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resident_id")
    private Resident resident;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "phone", length = 20)
    private String phone;

    @Column(name = "purpose", length = 200)
    private String purpose;

    @Column(name = "vehicle_no", length = 30)
    private String vehicleNo;

    @Column(name = "expected_at")
    private LocalDateTime expectedAt;

    @Column(name = "otp", length = 10)
    private String otp;

    @Column(name = "qr_payload", length = 500)
    private String qrPayload;

    @Column(name = "is_frequent", nullable = false)
    @Builder.Default
    private Boolean isFrequent = false;

    @Column(name = "frequency_days")
    private Integer frequencyDays;

    @Column(name = "photo_url", length = 500)
    private String photoUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private VisitorStatus status;

    @Column(name = "checked_in_at")
    private LocalDateTime checkedInAt;

    @Column(name = "checked_out_at")
    private LocalDateTime checkedOutAt;
}
