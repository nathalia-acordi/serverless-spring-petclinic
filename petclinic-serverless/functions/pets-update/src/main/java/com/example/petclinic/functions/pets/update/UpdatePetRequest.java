package com.example.petclinic.functions.pets.update;

import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UpdatePetRequest {

	@JsonProperty("id")
	private Integer id;

	@JsonProperty("name")
	private String name;

	@JsonProperty("birthDate")
	private LocalDate birthDate;

	@JsonProperty("ownerId")
	private Integer ownerId;

	@JsonProperty("typeId")
	private Integer typeId;

}
