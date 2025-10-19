package com.example.petclinic.domain.owner;

public class OwnerValidationException extends RuntimeException {
    public OwnerValidationException(String message) {
        super(message);
    }
}
