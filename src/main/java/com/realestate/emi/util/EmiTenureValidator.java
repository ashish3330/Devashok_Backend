package com.realestate.emi.util;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.Set;

public class EmiTenureValidator implements ConstraintValidator<ValidEmiTenure, Integer> {

    private static final Set<Integer> ALLOWED_TENURES = Set.of(6, 12, 24, 48, 60);

    @Override
    public boolean isValid(Integer value, ConstraintValidatorContext context) {
        if (value == null) {
            return true; // Let @NotNull handle null case
        }
        return ALLOWED_TENURES.contains(value);
    }
}
