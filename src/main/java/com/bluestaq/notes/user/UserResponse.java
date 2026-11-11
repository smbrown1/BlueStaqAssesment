package com.bluestaq.notes.user;

import java.time.Instant;
import java.util.UUID;

/** A user as returned by the API. It never includes the password or its hash. */
public record UserResponse(UUID id, String username, Instant createdAt) {

	static UserResponse from(User user) {
		return new UserResponse(user.getId(), user.getUsername(), user.getCreatedAt());
	}

}
