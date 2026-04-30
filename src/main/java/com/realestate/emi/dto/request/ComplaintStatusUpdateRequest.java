package com.realestate.emi.dto.request;

import com.realestate.emi.enums.ComplaintStatus;
import jakarta.validation.constraints.NotNull;
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
public class ComplaintStatusUpdateRequest {

    @NotNull
    private ComplaintStatus status;

    private String resolutionNotes;
}
