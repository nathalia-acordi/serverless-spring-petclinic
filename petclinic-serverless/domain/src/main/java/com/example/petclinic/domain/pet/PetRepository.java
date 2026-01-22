package com.example.petclinic.domain.pet;

import java.util.List;

public interface PetRepository {

	Pet save(Pet pet);

	Pet findById(Integer id);

	List<Pet> findByOwnerId(Integer ownerId);

	Pet findByOwnerIdAndName(Integer ownerId, String name);

}
