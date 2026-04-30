package com.realestate.emi.dto.response;

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
public class ResidentLoginResponse {

    private String accessToken;

    @Builder.Default
    private String tokenType = "Bearer";

    private Long expiresInMillis;

    private ResidentSummary resident;

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ResidentSummary {
        private Long id;
        private String fullName;
        private String primaryPhone;
        private String email;
        private FlatSummary flat;
        private OrganizationSummary organization;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FlatSummary {
        private Long id;
        private String flatNumber;
        private Integer floor;
        private Long blockId;
        private String blockName;
        private String blockCode;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrganizationSummary {
        private Long id;
        private String name;
        private String code;
    }
}
