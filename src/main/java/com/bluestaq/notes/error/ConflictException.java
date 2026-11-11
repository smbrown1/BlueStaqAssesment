package com.bluestaq.notes.error;

/** The request cannot be carried out because it clashes with existing data. Reported as 409. */
public class ConflictException extends RuntimeException {

	public ConflictException(String message) {
		super(message);
	}

}
