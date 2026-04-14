package com.realestate.emi.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "installment_plan_templates")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InstallmentPlanTemplate extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "phase_name", nullable = false)
    private String phaseName;

    @Column(name = "phase_order", nullable = false)
    private Integer phaseOrder;

    @Column(name = "percentage_of_total", nullable = false, precision = 5, scale = 2)
    private BigDecimal percentageOfTotal;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;
}
