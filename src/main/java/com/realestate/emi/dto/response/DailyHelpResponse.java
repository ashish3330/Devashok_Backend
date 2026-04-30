package com.realestate.emi.dto.response;

import com.realestate.emi.enums.DailyHelpType;
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
public class DailyHelpResponse {

    private Long id;
    private String name;
    private DailyHelpType helpType;
    private String primaryPhone;
    private String photoUrl;
    private String idProofType;
    private String idProofNumber;
    private Boolean isVerified;
    private LocalDateTime createdAt;
}
