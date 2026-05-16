package com.realestate.emi.dto.request;

import com.realestate.emi.enums.FamilyRelation;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
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
public class FamilyMemberRequest {

    @NotBlank
    @Size(max = 200)
    private String fullName;

    @NotNull
    private FamilyRelation relation;

    @Min(0)
    @Max(150)
    private Integer age;

    @Pattern(regexp = "^\\+?[1-9]\\d{9,14}$", message = "Invalid phone number")
    @Size(max = 20)
    private String phone;

    @Email
    @Size(max = 120)
    private String email;
}
