package com.example.petclinic.domain.owner;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Value;

/**
 * Domain aggregate root for Owner (simplified / decoupled from original Petclinic entity)
 * Only keeps fields needed for CRUD demonstration. Additional fields can be migrated incrementally.
 */
@Value
@Builder(toBuilder = true)
public class Owner {
    Long id; // null for create

    @NotBlank
    @Size(max = 30)
    String firstName;

    @NotBlank
    @Size(max = 30)
    String lastName;

    @Size(max = 255)
    String address;

    @Size(max = 80)
    String city;

    @Size(max = 20)
    String telephone;
}
