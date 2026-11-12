package com.bluestaq.notes.note;

import java.time.Instant;
import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;

/** A note as returned by the API. */
public record NoteResponse(
	UUID id,
	String title,
	String body,

	@Schema(description = "Counts saves. Send it back when updating the note, so a stale copy cannot overwrite a newer one.")
	long version,

	Owner owner,
	Instant createdAt,
	Instant updatedAt) {

	/** The user who wrote the note. */
	public record Owner(UUID id, String username) {
	}

	/** Must be called inside a transaction, because it reads the note's owner. */
	static NoteResponse from(Note note) {
		Owner owner = new Owner(note.getOwner().getId(), note.getOwner().getUsername());
		return new NoteResponse(note.getId(), note.getTitle(), note.getBody(), note.getVersion(), owner,
			note.getCreatedAt(), note.getUpdatedAt());
	}

}
