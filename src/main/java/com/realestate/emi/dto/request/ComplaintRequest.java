package com.realestate.emi.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ComplaintRequest {

    private Long categoryId;

    @NotBlank
    @Size(max = 200)
    private String title;

    private String description;

    private List<String> photoUrls;
}
