package com.realestate.emi.entity;

import com.realestate.emi.enums.DailyHelpType;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "society_daily_helps", uniqueConstraints = {
        @UniqueConstraint(name = "uk_daily_help_org_phone", columnNames = {"organization_id", "primary_phone"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DailyHelp extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "help_type", nullable = false, length = 20)
    private DailyHelpType helpType;

    @Column(name = "primary_phone", nullable = false, length = 20)
    private String primaryPhone;

    @Column(name = "photo_url", length = 500)
    private String photoUrl;

    @Column(name = "id_proof_type", length = 50)
    private String idProofType;

    @Column(name = "id_proof_number", length = 50)
    private String idProofNumber;

    @Column(name = "is_verified", nullable = false)
    @Builder.Default
    private Boolean isVerified = false;
}
