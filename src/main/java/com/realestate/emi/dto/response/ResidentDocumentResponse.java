package com.realestate.emi.dto.response;

import com.realestate.emi.enums.ResidentDocumentCategory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResidentDocumentResponse {

    private Long id;
    private String title;
    private ResidentDocumentCategory category;
    private String fileUrl;
    private Boolean visibleToResidents;
    private Long uploadedByUserId;
    private LocalDateTime createdAt;
}
