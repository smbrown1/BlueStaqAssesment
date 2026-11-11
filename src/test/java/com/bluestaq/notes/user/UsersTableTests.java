package com.bluestaq.notes.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

/** Checks the schema that the Flyway migration creates for users. */
@SpringBootTest
@Transactional
class UsersTableTests {

	private static final String INSERT =
		"insert into users (id, username, password_hash, created_at) values (?, ?, ?, ?)";

	@Autowired
	private JdbcTemplate jdbc;

	@Test
	void storesAUser() {
		UUID id = UUID.randomUUID();

		jdbc.update(INSERT, id, "alice", "hash", OffsetDateTime.now());

		String username = jdbc.queryForObject("select username from users where id = ?", String.class, id);
		assertThat(username).isEqualTo("alice");
	}

	@Test
	void rejectsADuplicateUsername() {
		jdbc.update(INSERT, UUID.randomUUID(), "alice", "hash", OffsetDateTime.now());

		assertThatThrownBy(() -> jdbc.update(INSERT, UUID.randomUUID(), "alice", "hash", OffsetDateTime.now()))
			.isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void rejectsAUserWithoutAUsername() {
		assertThatThrownBy(() -> jdbc.update(INSERT, UUID.randomUUID(), null, "hash", OffsetDateTime.now()))
			.isInstanceOf(DataIntegrityViolationException.class);
	}

}
