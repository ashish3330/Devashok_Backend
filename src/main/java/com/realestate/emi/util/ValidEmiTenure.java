package com.realestate.emi.util;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = EmiTenureValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidEmiTenure {

    String message() default "EMI tenure must be one of: 6, 12, 24, 48, 60 months";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
