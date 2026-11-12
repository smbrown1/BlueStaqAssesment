package com.bluestaq.notes.error;

/**
 * The caller knows the thing exists but is not allowed to do this to it. Reported as 403.
 * Where existence itself should stay hidden, {@link NotFoundException} is used instead (decision 026).
 */
public class ForbiddenException extends RuntimeException {

	public ForbiddenException(String message) {
		super(message);
	}

}
