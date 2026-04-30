package com.realestate.emi.dto.request;

import com.realestate.emi.enums.ResidentDocumentCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResidentDocumentRequest {

    @NotBlank
    @Size(max = 200)
    private String title;

    @NotNull
    private ResidentDocumentCategory category;

    @NotBlank
    @Size(max = 500)
    private String fileUrl;

    private Boolean visibleToResidents;
}
