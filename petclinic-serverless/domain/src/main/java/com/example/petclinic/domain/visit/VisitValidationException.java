package com.example.petclinic.domain.visit;

/**
 * Exception thrown when visit validation fails.
 */
public class VisitValidationException extends RuntimeException {
    public VisitValidationException(String message) {
        super(message);
    }
}
