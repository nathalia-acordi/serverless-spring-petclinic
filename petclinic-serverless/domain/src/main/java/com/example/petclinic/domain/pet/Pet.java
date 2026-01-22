package com.example.petclinic.domain.pet;

import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Pet {

	private Integer id;

	private String name;

	private LocalDate birthDate;

	private Integer ownerId;

	private PetType type;

}
