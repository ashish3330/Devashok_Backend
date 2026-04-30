package com.realestate.emi.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "society_complaint_categories", uniqueConstraints = {
        @UniqueConstraint(name = "uk_complaint_category_org_name", columnNames = {"organization_id", "name"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ComplaintCategory extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "sla_hours", nullable = false)
    private Integer slaHours;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;
}
