package com.example.petclinic.domain.owner;

public class OwnerNotFoundException extends RuntimeException {
    public OwnerNotFoundException(Long id) {
        super("Owner not found: " + id);
    }
}
