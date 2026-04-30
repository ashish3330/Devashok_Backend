package com.realestate.emi.dto.request;

import com.realestate.emi.enums.NoticeCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
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
public class NoticeRequest {

    @NotBlank
    @Size(max = 200)
    private String title;

    private String body;

    @NotNull
    private NoticeCategory category;

    private List<Long> targetBlockIds;

    private LocalDateTime publishedAt;

    private LocalDateTime expiresAt;

    @Size(max = 500)
    private String attachmentUrl;
}
