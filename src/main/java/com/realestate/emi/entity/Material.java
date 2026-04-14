package com.realestate.emi.entity;

import com.realestate.emi.enums.MaterialCategory;
import com.realestate.emi.enums.UnitType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "materials")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Material extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 20)
    private MaterialCategory category;

    @Enumerated(EnumType.STRING)
    @Column(name = "unit", nullable = false, length = 20)
    private UnitType unit;

    @Column(name = "unit_cost", nullable = false, precision = 19, scale = 2)
    private BigDecimal unitCost;

    @Column(name = "hsn_code", length = 20)
    private String hsnCode;

    @Column(name = "reorder_level", nullable = false)
    @Builder.Default
    private BigDecimal reorderLevel = BigDecimal.ZERO;

    @Column(name = "minimum_threshold", nullable = false)
    @Builder.Default
    private BigDecimal minimumThreshold = BigDecimal.ZERO;

    @Column(name = "current_quantity", nullable = false, precision = 19, scale = 2)
    @Builder.Default
    private BigDecimal currentQuantity = BigDecimal.ZERO;

    @ManyToMany(mappedBy = "materials", fetch = FetchType.LAZY)
    @Builder.Default
    private List<Supplier> suppliers = new ArrayList<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id")
    private Organization organization;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;
}
