package com.bluestaq.notes.team;

import java.time.Instant;
import java.util.UUID;

import com.bluestaq.notes.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;

/** A user's membership of a team. */
@Entity
@Table(name = "team_members")
public class TeamMember {

	@EmbeddedId
	private TeamMemberId id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@MapsId("userId")
	@JoinColumn(name = "user_id")
	private User user;

	@Column(name = "added_at", nullable = false, updatable = false)
	private Instant addedAt;

	/** For JPA. */
	protected TeamMember() {
	}

	public TeamMember(UUID teamId, User user) {
		this.id = new TeamMemberId(teamId, user.getId());
		this.user = user;
		this.addedAt = Instant.now();
	}

	public User getUser() {
		return user;
	}

	public Instant getAddedAt() {
		return addedAt;
	}

}
