package com.example.petclinic.functions.pets.create;

import java.util.function.Function;

import org.springframework.stereotype.Component;

import com.example.petclinic.domain.pet.Pet;
import com.example.petclinic.domain.pet.PetService;
import com.example.petclinic.domain.pet.PetType;

import software.amazon.lambda.powertools.logging.Logging;
import software.amazon.lambda.powertools.tracing.Tracing;

@Component
public class CreatePetFunction implements Function<CreatePetRequest, Pet> {

	private final PetService petService;

	public CreatePetFunction(PetService petService) {
		this.petService = petService;
	}

	@Override
	@Logging
	@Tracing
	public Pet apply(CreatePetRequest request) {
		Pet pet = new Pet();
		pet.setName(request.getName());
		pet.setBirthDate(request.getBirthDate());
		pet.setOwnerId(request.getOwnerId());

		if (request.getTypeId() != null) {
			PetType type = new PetType();
			type.setId(request.getTypeId());
			pet.setType(type);
		}

		return petService.create(pet);
	}

}
