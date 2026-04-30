package com.realestate.emi.entity;

import com.realestate.emi.enums.ResidentDocumentCategory;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "society_resident_documents", indexes = {
        @Index(name = "idx_resident_doc_org_category", columnList = "organization_id, category")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResidentDocument extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 30)
    private ResidentDocumentCategory category;

    @Column(name = "file_url", nullable = false, length = 500)
    private String fileUrl;

    @Column(name = "uploaded_by_user_id")
    private Long uploadedByUserId;

    @Column(name = "visible_to_residents", nullable = false)
    @Builder.Default
    private Boolean visibleToResidents = true;
}
