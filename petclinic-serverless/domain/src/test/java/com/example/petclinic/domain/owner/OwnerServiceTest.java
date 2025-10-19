package com.example.petclinic.domain.owner;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.validation.Validation;
import jakarta.validation.Validator;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class OwnerServiceTest {

    private InMemoryRepo repo;
    private OwnerService service;

    @BeforeEach
    void setup() {
        repo = new InMemoryRepo();
        Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
        service = new OwnerService(repo, validator);
    }

    @Test
    void createAndGet() {
        Owner created = service.create(Owner.builder().firstName("A").lastName("B").build());
        assertNotNull(created.getId());
        assertTrue(service.get(created.getId()).isPresent());
    }

    @Test
    void updateNonExistingThrows() {
        assertThrows(OwnerNotFoundException.class, () -> service.update(999L, Owner.builder().firstName("X").lastName("Y").build()));
    }

    @Test
    void deleteRemoves() {
        Owner created = service.create(Owner.builder().firstName("A").lastName("B").build());
        service.delete(created.getId());
        assertTrue(service.list(0,10).isEmpty());
    }

    // Simple in-memory implementation for tests
    static class InMemoryRepo implements OwnerRepository {
        private final Map<Long, Owner> db = new HashMap<>();
        private long seq = 1;

        @Override public Owner save(Owner owner) { if (owner.getId()==null) owner = owner.toBuilder().id(seq++).build(); db.put(owner.getId(), owner); return owner; }
        @Override public Optional<Owner> findById(Long id) { return Optional.ofNullable(db.get(id)); }
        @Override public List<Owner> findAll(int page, int size) { return new ArrayList<>(db.values()); }
        @Override public void deleteById(Long id) { db.remove(id); }
        @Override public boolean existsByTelephone(String telephone) { return db.values().stream().anyMatch(o -> Objects.equals(o.getTelephone(), telephone)); }
        @Override public boolean existsById(Long id) { return db.containsKey(id); }
        @Override public boolean existsByTelephoneExcludingId(String telephone, Long excludeId) {
            return db.values().stream().anyMatch(o -> !Objects.equals(o.getId(), excludeId) && Objects.equals(o.getTelephone(), telephone));
        }
    }
}
