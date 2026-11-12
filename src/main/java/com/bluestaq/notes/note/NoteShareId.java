package com.bluestaq.notes.note;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/** The primary key of a share: one note shared with one team. */
@Embeddable
public record NoteShareId(
	@Column(name = "note_id") UUID noteId,
	@Column(name = "team_id") UUID teamId) {
}
