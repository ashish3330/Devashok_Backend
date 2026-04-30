package com.realestate.emi.entity;

import com.realestate.emi.enums.FlatType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "society_flats", uniqueConstraints = {
        @UniqueConstraint(name = "uk_flat_org_block_number", columnNames = {"organization_id", "block_id", "flat_number"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Flat extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "block_id", nullable = false)
    private Block block;

    @Column(name = "flat_number", nullable = false, length = 30)
    private String flatNumber;

    @Column(name = "floor")
    private Integer floor;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", length = 20)
    private FlatType type;

    @Column(name = "sqft", precision = 10, scale = 2)
    private BigDecimal sqft;

    @Column(name = "owner_name", length = 200)
    private String ownerName;

    @Column(name = "is_occupied", nullable = false)
    @Builder.Default
    private Boolean isOccupied = false;
}
