package com.bluestaq.notes.team;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/** The body of a request to add a user to a team. */
public record AddMemberRequest(

	@Schema(description = "The username of a registered user. Case is ignored.", example = "bob")
	@NotBlank
	String username) {
}
