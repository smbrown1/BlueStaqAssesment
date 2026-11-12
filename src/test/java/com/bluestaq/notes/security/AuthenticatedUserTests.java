package com.bluestaq.notes.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.Test;

class AuthenticatedUserTests {

	@Test
	void toStringLeavesOutThePasswordHash() {
		AuthenticatedUser user = new AuthenticatedUser(UUID.randomUUID(), "alice", "$2a$10$secret-hash");

		assertThat(user.toString())
			.contains("alice")
			.doesNotContain("secret-hash");
	}

}
