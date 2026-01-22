package com.example.petclinic.domain.vet;

import java.util.List;
import java.util.Optional;

/**
 * Port (interface) for Vet persistence.
 */
public interface VetRepository {
    List<Vet> findAll(int page, int size);

    Optional<Vet> findById(Long id);
    
    List<Specialty> findSpecialtiesByVetId(Long vetId);
}
