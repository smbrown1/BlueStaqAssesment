package com.bluestaq.notes.team;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/** The primary key of a team membership: one user in one team. */
@Embeddable
public record TeamMemberId(
	@Column(name = "team_id") UUID teamId,
	@Column(name = "user_id") UUID userId) {
}
