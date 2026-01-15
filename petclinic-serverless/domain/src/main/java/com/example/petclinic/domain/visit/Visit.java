package com.example.petclinic.domain.visit;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * Domain entity representing a pet visit.
 * Associates a visit with an owner/pet and captures visit details.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class Visit {
    private Long id;
    private Long ownerId;
    private Long petId;
    private LocalDate visitDate;
    private String description;

    public Visit(Long ownerId, Long petId, LocalDate visitDate, String description) {
        this.ownerId = ownerId;
        this.petId = petId;
        this.visitDate = visitDate;
        this.description = description;
    }
}
