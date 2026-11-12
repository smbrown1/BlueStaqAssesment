package com.bluestaq.notes.error;

/**
 * The thing asked for does not exist, or the caller is not allowed to know that it does.
 * Reported as 404.
 */
public class NotFoundException extends RuntimeException {

	public NotFoundException(String message) {
		super(message);
	}

}
