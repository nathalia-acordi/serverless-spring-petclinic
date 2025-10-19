package com.example.petclinic.api.common.validation;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;

import java.util.Set;
import java.util.stream.Collectors;

public final class ValidationSupport {
    private static final Validator VALIDATOR = Validation.buildDefaultValidatorFactory().getValidator();
    private ValidationSupport() {}

    public static <T> void validate(T obj) {
        if (obj == null) throw new ValidationSupportException("Request body is null");
        Set<ConstraintViolation<T>> violations = VALIDATOR.validate(obj);
        if (!violations.isEmpty()) {
            String msg = violations.stream()
                    .map(v -> v.getPropertyPath() + " " + v.getMessage())
                    .collect(Collectors.joining("; "));
            throw new ValidationSupportException(msg);
        }
    }
}
