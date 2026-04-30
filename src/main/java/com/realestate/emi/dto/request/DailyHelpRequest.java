package com.realestate.emi.dto.request;

import com.realestate.emi.enums.DailyHelpType;
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
public class DailyHelpRequest {

    @NotBlank
    @Size(max = 200)
    private String name;

    @NotNull
    private DailyHelpType helpType;

    @NotBlank
    @Size(max = 20)
    private String primaryPhone;

    @Size(max = 500)
    private String photoUrl;

    @Size(max = 50)
    private String idProofType;

    @Size(max = 50)
    private String idProofNumber;

    private Boolean isVerified;
}
