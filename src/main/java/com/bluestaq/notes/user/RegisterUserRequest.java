package com.bluestaq.notes.user;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** The body of a registration request. */
public record RegisterUserRequest(

	@Schema(description = "3 to 50 characters: letters, digits, dots, underscores and hyphens. Stored in lower case.",
		example = "alice")
	@NotNull
	@Size(min = 3, max = 50)
	@Pattern(regexp = "[A-Za-z0-9._-]*", message = "may only contain letters, digits, dots, underscores and hyphens")
	String username,

	@Schema(description = "8 to 64 characters, and at most 72 bytes when encoded as UTF-8.", example = "correct-horse")
	@NotNull
	@Size(min = 8, max = 64)
	@MaxUtf8Bytes(72)
	String password) {
}
