package com.example.petclinic.domain.visit;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import java.time.LocalDate;
import java.util.List;

/**
 * Application service containing core business rules for Visits.
 */
@RequiredArgsConstructor
public class VisitService {
    private final VisitRepository repository;

    public Visit create(@Valid Visit visit) {
        validateVisit(visit);
        return repository.save(visit);
    }

    public Visit get(Long visitId) {
        return repository.findById(visitId).orElseThrow(() -> new VisitNotFoundException(visitId));
    }

    public Visit getByIdAndOwnerId(Long visitId, Long ownerId) {
        return repository.findByIdAndOwnerId(visitId, ownerId)
                .orElseThrow(() -> new VisitNotFoundException(visitId, ownerId));
    }

    public List<Visit> listByPet(Long ownerId, Long petId) {
        return repository.findByOwnerIdAndPetId(ownerId, petId);
    }

    public List<Visit> list(int page, int size) {
        return repository.findAll(page, size);
    }

    public Visit update(Long visitId, Long ownerId, @Valid Visit updated) {
        Visit existing = repository.findByIdAndOwnerId(visitId, ownerId)
                .orElseThrow(() -> new VisitNotFoundException(visitId, ownerId));

        Visit toSave = existing.toBuilder()
                .visitDate(updated.getVisitDate() != null ? updated.getVisitDate() : existing.getVisitDate())
                .description(updated.getDescription() != null ? updated.getDescription() : existing.getDescription())
                .build();

        validateVisit(toSave);
        return repository.save(toSave);
    }

    public void delete(Long visitId) {
        repository.deleteById(visitId);
    }

    public void deleteByIdAndOwnerId(Long visitId, Long ownerId) {
        repository.deleteByIdAndOwnerId(visitId, ownerId);
    }

    private void validateVisit(Visit visit) {
        if (visit.getVisitDate() == null) {
            throw new VisitValidationException("Visit date is required");
        }
        if (visit.getVisitDate().isAfter(LocalDate.now())) {
            throw new VisitValidationException("Visit date cannot be in the future");
        }
        if (visit.getDescription() == null || visit.getDescription().isBlank()) {
            throw new VisitValidationException("Visit description is required");
        }
        if (visit.getDescription().length() > 255) {
            throw new VisitValidationException("Visit description too long (max 255 characters)");
        }
    }
}
