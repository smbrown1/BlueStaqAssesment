package com.bluestaq.notes.team;

import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

import com.bluestaq.notes.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/** A team: a named group of users that notes can be shared with. */
@Entity
@Table(name = "teams")
public class Team {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	/** The name as the creator typed it, for display. */
	@Column(nullable = false, length = 50)
	private String name;

	/** The name in lower case. Unique, so two teams cannot differ only by case (decision 038). */
	@Column(name = "name_key", nullable = false, unique = true, length = 50)
	private String nameKey;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "created_by", nullable = false, updatable = false)
	private User createdBy;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	/** For JPA. */
	protected Team() {
	}

	public Team(String name, User createdBy) {
		this.name = name;
		this.nameKey = keyFor(name);
		this.createdBy = createdBy;
		this.createdAt = Instant.now();
	}

	/** The form of a name used to decide whether two names are the same. */
	public static String keyFor(String name) {
		return name.toLowerCase(Locale.ROOT);
	}

	public UUID getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

}
