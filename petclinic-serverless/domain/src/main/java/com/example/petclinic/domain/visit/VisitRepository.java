package com.example.petclinic.domain.visit;

import java.util.List;
import java.util.Optional;

/**
 * Port (interface) for Visit persistence.
 * Implementation provided by infra-rds module via JdbcVisitRepository.
 */
public interface VisitRepository {
    Visit save(Visit visit);

    Optional<Visit> findById(Long id);

    Optional<Visit> findByIdAndOwnerId(Long visitId, Long ownerId);

    List<Visit> findByOwnerIdAndPetId(Long ownerId, Long petId);

    List<Visit> findAll(int page, int size);

    void deleteById(Long id);

    void deleteByIdAndOwnerId(Long visitId, Long ownerId);
}
