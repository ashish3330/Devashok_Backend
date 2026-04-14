package com.realestate.emi.dto.response;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SupplierResponse {

    private Long id;
    private String name;
    private String contactPerson;
    private String phone;
    private String email;
    private String gstNumber;
    private String address;
    private Boolean isActive;
    private java.util.List<MaterialSummary> materials;

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MaterialSummary {
        private Long id;
        private String name;
        private String category;
        private String unit;
    }
}
