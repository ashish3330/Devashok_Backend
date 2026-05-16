package com.realestate.emi.entity;

import com.realestate.emi.enums.VehicleType;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "society_vehicles",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_vehicle_org_number", columnNames = {"organization_id", "vehicle_number"})
        },
        indexes = {
                @Index(name = "idx_vehicle_org_resident", columnList = "organization_id, resident_id"),
                @Index(name = "idx_vehicle_active", columnList = "is_active")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Vehicle extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "resident_id", nullable = false)
    private Resident resident;

    @Column(name = "vehicle_number", nullable = false, length = 30)
    private String vehicleNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "vehicle_type", nullable = false, length = 20)
    private VehicleType vehicleType;

    @Column(name = "make", length = 60)
    private String make;

    @Column(name = "model", length = 60)
    private String model;

    @Column(name = "color", length = 40)
    private String color;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;
}
