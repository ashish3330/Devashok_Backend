package com.realestate.emi.dto.response;

import com.realestate.emi.enums.NoticeCategory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NoticeResponse {

    private Long id;
    private String title;
    private String body;
    private NoticeCategory category;
    private List<Long> targetBlockIds;
    private LocalDateTime publishedAt;
    private LocalDateTime expiresAt;
    private String attachmentUrl;
    private Long postedByUserId;
    private LocalDateTime createdAt;
}
