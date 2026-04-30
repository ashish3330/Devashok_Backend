package com.realestate.emi.entity;

import com.realestate.emi.enums.ResidentPhoneLabel;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "society_resident_phones", uniqueConstraints = {
        @UniqueConstraint(name = "uk_resident_phone_org_phone", columnNames = {"organization_id", "phone"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResidentPhone extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "resident_id", nullable = false)
    private Resident resident;

    @Column(name = "phone", nullable = false, length = 20)
    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(name = "label", nullable = false, length = 20)
    private ResidentPhoneLabel label;

    @Column(name = "is_verified", nullable = false)
    @Builder.Default
    private Boolean isVerified = false;
}
