package com.example.petclinic.domain.visit;

/**
 * Exception thrown when a visit is not found.
 */
public class VisitNotFoundException extends RuntimeException {
    public VisitNotFoundException(Long visitId) {
        super("Visit not found with id: " + visitId);
    }

    public VisitNotFoundException(Long visitId, Long ownerId) {
        super("Visit not found with id: " + visitId + " for owner: " + ownerId);
    }
}
