package com.example.petclinic.domain.vet;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Domain entity representing a veterinarian.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class Vet {
    private Long id;
    private String firstName;
    private String lastName;
    
    @Builder.Default
    private List<Specialty> specialties = new ArrayList<>();

    public Vet(String firstName, String lastName) {
        this.firstName = firstName;
        this.lastName = lastName;
    }
}
