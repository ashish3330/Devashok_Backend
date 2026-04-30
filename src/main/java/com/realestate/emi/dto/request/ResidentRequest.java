package com.realestate.emi.dto.request;

import com.realestate.emi.enums.ResidentType;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class ResidentRequest {

    @NotNull(message = "Flat id is required")
    private Long flatId;

    @NotBlank(message = "Full name is required")
    @Size(max = 200)
    private String fullName;

    @Email(message = "Email must be valid")
    @Size(max = 120)
    private String email;

    @NotBlank(message = "Primary phone is required")
    @Pattern(regexp = "^\\+?[1-9]\\d{9,14}$", message = "Invalid mobile number")
    private String primaryPhone;

    @NotNull(message = "Resident type is required")
    private ResidentType residentType;

    private LocalDate moveInDate;

    private LocalDate moveOutDate;

    /**
     * Optional list of additional phones (whitelisted) created with the resident.
     * primaryPhone is auto-added with label PRIMARY — do not duplicate it here.
     */
    private List<@Pattern(regexp = "^\\+?[1-9]\\d{9,14}$") String> phones;
}
