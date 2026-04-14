package com.realestate.emi.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "suppliers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Supplier extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "contact_person")
    private String contactPerson;

    @Column(name = "phone", length = 15)
    private String phone;

    @Column(name = "email")
    private String email;

    @Column(name = "gst_number", length = 20)
    private String gstNumber;

    @Column(name = "address", columnDefinition = "TEXT")
    private String address;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "supplier_materials",
            joinColumns = @JoinColumn(name = "supplier_id"),
            inverseJoinColumns = @JoinColumn(name = "material_id")
    )
    @Builder.Default
    private List<Material> materials = new ArrayList<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id")
    private Organization organization;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;
}
