package com.example.petclinic.domain.owner;

import java.util.List;
import java.util.Optional;

/**
 * Repository Port (Hexagonal) to be implemented by infra layer (RDS / JPA / JDBC) or mocked for tests.
 */
public interface OwnerRepository {
    Owner save(Owner owner);
    Optional<Owner> findById(Long id);
    List<Owner> findAll(int page, int size);
    void deleteById(Long id);
    boolean existsByTelephone(String telephone);
    boolean existsById(Long id);
    boolean existsByTelephoneExcludingId(String telephone, Long excludeId);
}
