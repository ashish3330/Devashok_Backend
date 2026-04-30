package com.realestate.emi.entity;

import com.realestate.emi.enums.NoticeCategory;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "society_notices", indexes = {
        @Index(name = "idx_notice_org_published", columnList = "organization_id, published_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notice extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "body", columnDefinition = "TEXT")
    private String body;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 20)
    private NoticeCategory category;

    @Column(name = "target_block_ids", columnDefinition = "TEXT")
    private String targetBlockIds;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(name = "attachment_url", length = 500)
    private String attachmentUrl;

    @Column(name = "posted_by_user_id")
    private Long postedByUserId;
}
