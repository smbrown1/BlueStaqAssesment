package com.bluestaq.notes.note;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.OffsetDateTime;
import java.util.UUID;

import com.bluestaq.notes.support.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

/** Checks the schema that the Flyway migration creates for notes. */
class NotesTableTests extends IntegrationTest {

	private static final String INSERT = """
		insert into notes (id, owner_id, title, body, version, created_at, updated_at)
		values (?, ?, ?, ?, 0, ?, ?)
		""";

	private void insertNote(UUID id, UUID ownerId, String title, String body) {
		OffsetDateTime now = OffsetDateTime.now();
		jdbc.update(INSERT, id, ownerId, title, body, now, now);
	}

	@Test
	void storesANoteForAUser() throws Exception {
		UUID owner = UUID.fromString(registerUser("alice"));
		UUID id = UUID.randomUUID();

		insertNote(id, owner, "Standup", "Shipped the migration.");

		String title = jdbc.queryForObject("select title from notes where id = ?", String.class, id);
		assertThat(title).isEqualTo("Standup");
	}

	@Test
	void storesABodyOf10000Characters() throws Exception {
		UUID owner = UUID.fromString(registerUser("alice"));
		UUID id = UUID.randomUUID();

		insertNote(id, owner, "Long", "x".repeat(10_000));

		Integer length = jdbc.queryForObject("select char_length(body) from notes where id = ?", Integer.class, id);
		assertThat(length).isEqualTo(10_000);
	}

	@Test
	void rejectsANoteWhoseOwnerDoesNotExist() {
		assertThatThrownBy(() -> insertNote(UUID.randomUUID(), UUID.randomUUID(), "Orphan", ""))
			.isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void rejectsANoteWithoutATitle() throws Exception {
		UUID owner = UUID.fromString(registerUser("alice"));

		assertThatThrownBy(() -> insertNote(UUID.randomUUID(), owner, null, ""))
			.isInstanceOf(DataIntegrityViolationException.class);
	}

}
