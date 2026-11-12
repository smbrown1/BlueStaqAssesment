package com.bluestaq.notes.note;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** The body of a request to create a note. */
public record CreateNoteRequest(

	@Schema(description = "Required, up to 200 characters, and not only spaces.", example = "Standup notes")
	@NotBlank
	@Size(max = 200)
	String title,

	@Schema(description = "Up to 10,000 characters. Empty if left out.", example = "Shipped the migration.")
	@Size(max = 10_000)
	String body) {
}
