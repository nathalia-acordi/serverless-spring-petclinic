package com.example.petclinic.domain.vet;

import lombok.RequiredArgsConstructor;

import java.util.List;

/**
 * Application service for Vets operations.
 */
@RequiredArgsConstructor
public class VetService {
    private final VetRepository repository;

    public Vet get(Long vetId) {
        Vet vet = repository.findById(vetId)
                .orElseThrow(() -> new VetNotFoundException(vetId));
        
        List<Specialty> specialties = repository.findSpecialtiesByVetId(vetId);
        vet.setSpecialties(specialties);
        
        return vet;
    }

    public List<Vet> list(int page, int size) {
        List<Vet> vets = repository.findAll(page, size);
        
        for (Vet vet : vets) {
            List<Specialty> specialties = repository.findSpecialtiesByVetId(vet.getId());
            vet.setSpecialties(specialties);
        }
        
        return vets;
    }
}
