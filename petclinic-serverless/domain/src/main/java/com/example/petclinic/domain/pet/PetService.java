package com.example.petclinic.domain.pet;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;

@Service
public class PetService {

	private final PetRepository petRepository;

	public PetService(PetRepository petRepository) {
		this.petRepository = petRepository;
	}

	public Pet create(Pet pet) {
		validatePetCreation(pet);
		return petRepository.save(pet);
	}

	public Pet update(Pet pet) {
		validatePetUpdate(pet);
		return petRepository.save(pet);
	}

	public Pet findById(Integer id) {
		Pet pet = petRepository.findById(id);
		if (pet == null) {
			throw new PetNotFoundException("Pet not found with id: " + id);
		}
		return pet;
	}

	public List<Pet> findByOwnerId(Integer ownerId) {
		return petRepository.findByOwnerId(ownerId);
	}

	private void validatePetCreation(Pet pet) {
		if (pet.getOwnerId() == null) {
			throw new PetValidationException("Owner ID is required");
		}

		if (pet.getName() == null || pet.getName().isBlank()) {
			throw new PetValidationException("Pet name is required");
		}

		if (pet.getType() == null || pet.getType().getId() == null) {
			throw new PetValidationException("Pet type is required");
		}

		// Check if pet name already exists for the owner
		Pet existing = petRepository.findByOwnerIdAndName(pet.getOwnerId(), pet.getName());
		if (existing != null) {
			throw new PetValidationException("Pet name '" + pet.getName() + "' already exists for this owner");
		}

		// Birth date cannot be in the future
		if (pet.getBirthDate() != null && pet.getBirthDate().isAfter(LocalDate.now())) {
			throw new PetValidationException("Birth date cannot be in the future");
		}
	}

	private void validatePetUpdate(Pet pet) {
		if (pet.getId() == null) {
			throw new PetValidationException("Pet ID is required for update");
		}

		if (pet.getOwnerId() == null) {
			throw new PetValidationException("Owner ID is required");
		}

		if (pet.getName() == null || pet.getName().isBlank()) {
			throw new PetValidationException("Pet name is required");
		}

		if (pet.getType() == null || pet.getType().getId() == null) {
			throw new PetValidationException("Pet type is required");
		}

		// Check if pet with this ID exists
		Pet existing = petRepository.findById(pet.getId());
		if (existing == null) {
			throw new PetNotFoundException("Pet not found with id: " + pet.getId());
		}

		// Check if the new name already exists for another pet of the same owner
		Pet petWithName = petRepository.findByOwnerIdAndName(pet.getOwnerId(), pet.getName());
		if (petWithName != null && !petWithName.getId().equals(pet.getId())) {
			throw new PetValidationException("Pet name '" + pet.getName() + "' already exists for this owner");
		}

		// Birth date cannot be in the future
		if (pet.getBirthDate() != null && pet.getBirthDate().isAfter(LocalDate.now())) {
			throw new PetValidationException("Birth date cannot be in the future");
		}
	}

}
