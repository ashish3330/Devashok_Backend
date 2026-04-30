package com.realestate.emi.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "society_blocks", uniqueConstraints = {
        @UniqueConstraint(name = "uk_block_org_code", columnNames = {"organization_id", "code"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Block extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "code", nullable = false, length = 30)
    private String code;

    @Column(name = "total_floors")
    private Integer totalFloors;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;
}
