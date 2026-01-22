package com.example.petclinic.domain.vet;

/**
 * Exception thrown when a vet is not found.
 */
public class VetNotFoundException extends RuntimeException {
    public VetNotFoundException(Long vetId) {
        super("Vet not found with id: " + vetId);
    }
}
