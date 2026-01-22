package com.example.petclinic.domain.pet;

public class PetValidationException extends RuntimeException {

	public PetValidationException(String message) {
		super(message);
	}

	public PetValidationException(String message, Throwable cause) {
		super(message, cause);
	}

}
