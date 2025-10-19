package com.example.petclinic.domain.owner;

import jakarta.validation.Valid;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Optional;

/**
 * Application service containing core business rules for Owners.
 */
@RequiredArgsConstructor
public class OwnerService {
    private final OwnerRepository repository;
    private final Validator validator;

    public Owner create(@Valid Owner owner) {
        // Example business rule: telephone must be unique if provided
        if (owner.getTelephone() != null && repository.existsByTelephone(owner.getTelephone())) {
            throw new OwnerValidationException("Telephone already registered");
        }
        return repository.save(owner);
    }

    public Optional<Owner> get(Long id) {
        return repository.findById(id);
    }

    public List<Owner> list(int page, int size) {
        return repository.findAll(page, size);
    }

    public Owner update(Long id, @Valid Owner owner) {
        Owner existing = repository.findById(id).orElseThrow(() -> new OwnerNotFoundException(id));
        // If telephone changed and new telephone already belongs to a different owner -> validation error
        if (owner.getTelephone() != null && !owner.getTelephone().equals(existing.getTelephone())) {
            if (repository.existsByTelephoneExcludingId(owner.getTelephone(), existing.getId())) {
                throw new OwnerValidationException("Telephone already registered");
            }
        }
        Owner toSave = owner.toBuilder().id(existing.getId()).build();
        // Use repository.save (which will route to update path) or a dedicated update method; save is fine here.
        return repository.save(toSave);
    }

    public void delete(Long id) {
        repository.deleteById(id);
    }
}
