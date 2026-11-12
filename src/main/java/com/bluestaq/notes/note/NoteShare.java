package com.bluestaq.notes.note;

import java.time.Instant;
import java.util.UUID;

import com.bluestaq.notes.team.Team;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;

/** A note shared with a team. The team's members can read the note. */
@Entity
@Table(name = "note_shares")
public class NoteShare {

	@EmbeddedId
	private NoteShareId id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@MapsId("teamId")
	@JoinColumn(name = "team_id")
	private Team team;

	@Column(name = "shared_at", nullable = false, updatable = false)
	private Instant sharedAt;

	/** For JPA. */
	protected NoteShare() {
	}

	public NoteShare(UUID noteId, Team team) {
		this.id = new NoteShareId(noteId, team.getId());
		this.team = team;
		this.sharedAt = Instant.now();
	}

	public Team getTeam() {
		return team;
	}

	public Instant getSharedAt() {
		return sharedAt;
	}

}
