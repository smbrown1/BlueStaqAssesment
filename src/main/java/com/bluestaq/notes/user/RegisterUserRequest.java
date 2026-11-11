package com.bluestaq.notes.user;

import io.swagger.v3.oas.annotations.media.Schema;

/** The body of a registration request. */
public record RegisterUserRequest(

	@Schema(description = "Stored in lower case.", example = "alice")
	String username,

	@Schema(example = "correct-horse")
	String password) {
}
