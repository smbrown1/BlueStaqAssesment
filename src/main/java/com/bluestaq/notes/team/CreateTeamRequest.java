package com.bluestaq.notes.team;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** The body of a request to create a team. */
public record CreateTeamRequest(

	@Schema(description = "3 to 50 characters once spaces at either end are removed. Unique ignoring case.",
		example = "Platform Team")
	@NotBlank
	@Size(min = 3, max = 50)
	String name) {

	/** Removes spaces at either end before the limits are checked (decision 038). */
	public CreateTeamRequest {
		name = name == null ? null : name.strip();
	}

}
