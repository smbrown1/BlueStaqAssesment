package com.bluestaq.notes.note;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.OffsetDateTime;
import java.util.UUID;

import com.bluestaq.notes.support.IntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

/** Checks the schema that the Flyway migration creates for sharing notes with teams. */
class NoteSharesTableTests extends IntegrationTest {

	private UUID alice;
	private UUID noteId;
	private UUID teamId;

	@BeforeEach
	void aNoteAndATeam() throws Exception {
		alice = UUID.fromString(registerUser("alice"));
		noteId = UUID.randomUUID();
		OffsetDateTime now = OffsetDateTime.now();
		jdbc.update("""
			insert into notes (id, owner_id, title, body, version, created_at, updated_at)
			values (?, ?, 'Standup', '', 0, ?, ?)
			""", noteId, alice, now, now);
		teamId = UUID.randomUUID();
		jdbc.update("""
			insert into teams (id, name, name_key, created_by, created_at) values (?, ?, ?, ?, ?)
			""", teamId, "Platform", "platform", alice, now);
	}

	private void insertShare(UUID note, UUID team) {
		jdbc.update("insert into note_shares (note_id, team_id, shared_at) values (?, ?, ?)",
			note, team, OffsetDateTime.now());
	}

	@Test
	void storesAShare() {
		insertShare(noteId, teamId);

		Integer count = jdbc.queryForObject(
			"select count(*) from note_shares where note_id = ? and team_id = ?", Integer.class, noteId, teamId);
		assertThat(count).isEqualTo(1);
	}

	@Test
	void rejectsSharingTheSameNoteWithTheSameTeamTwice() {
		insertShare(noteId, teamId);

		assertThatThrownBy(() -> insertShare(noteId, teamId))
			.isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void rejectsAShareOfANoteThatDoesNotExist() {
		assertThatThrownBy(() -> insertShare(UUID.randomUUID(), teamId))
			.isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void rejectsAShareWithATeamThatDoesNotExist() {
		assertThatThrownBy(() -> insertShare(noteId, UUID.randomUUID()))
			.isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void deletingTheNoteRemovesItsShares() {
		insertShare(noteId, teamId);

		jdbc.update("delete from notes where id = ?", noteId);

		Integer count = jdbc.queryForObject("select count(*) from note_shares", Integer.class);
		assertThat(count).isZero();
	}

}
