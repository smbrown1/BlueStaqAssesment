package com.bluestaq.notes.note;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** The body of a request to replace a note's title and body. */
public record UpdateNoteRequest(

	@Schema(description = "Required, up to 200 characters, and not only spaces.", example = "Standup notes, Tuesday")
	@NotBlank
	@Size(max = 200)
	String title,

	@Schema(description = "Up to 10,000 characters. Empty if left out.", example = "Rolled the migration back.")
	@Size(max = 10_000)
	String body,

	@Schema(description = "The version the note had when the caller read it. If the note has been saved since, the update is rejected with 409.",
		example = "0")
	@NotNull
	Long version) {
}
