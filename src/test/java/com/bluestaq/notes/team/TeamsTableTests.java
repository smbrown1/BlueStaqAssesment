package com.bluestaq.notes.team;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.OffsetDateTime;
import java.util.UUID;

import com.bluestaq.notes.support.IntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

/** Checks the schema that the Flyway migration creates for teams and their members. */
class TeamsTableTests extends IntegrationTest {

	private UUID alice;

	@BeforeEach
	void registerAlice() throws Exception {
		alice = UUID.fromString(registerUser("alice"));
	}

	private UUID insertTeam(String name, String nameKey) {
		UUID id = UUID.randomUUID();
		jdbc.update("insert into teams (id, name, name_key, created_by, created_at) values (?, ?, ?, ?, ?)",
			id, name, nameKey, alice, OffsetDateTime.now());
		return id;
	}

	private void insertMember(UUID teamId, UUID userId) {
		jdbc.update("insert into team_members (team_id, user_id, added_at) values (?, ?, ?)",
			teamId, userId, OffsetDateTime.now());
	}

	@Test
	void storesATeamWithItsNameAsGiven() {
		UUID id = insertTeam("Platform Team", "platform team");

		String name = jdbc.queryForObject("select name from teams where id = ?", String.class, id);
		assertThat(name).isEqualTo("Platform Team");
	}

	@Test
	void rejectsTwoTeamsWithTheSameNameKey() {
		insertTeam("Platform", "platform");

		assertThatThrownBy(() -> insertTeam("PLATFORM", "platform"))
			.isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void rejectsATeamWhoseCreatorDoesNotExist() {
		assertThatThrownBy(() -> jdbc.update(
			"insert into teams (id, name, name_key, created_by, created_at) values (?, ?, ?, ?, ?)",
			UUID.randomUUID(), "Orphans", "orphans", UUID.randomUUID(), OffsetDateTime.now()))
			.isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void storesAMember() {
		UUID team = insertTeam("Platform", "platform");

		insertMember(team, alice);

		Integer count = jdbc.queryForObject(
			"select count(*) from team_members where team_id = ? and user_id = ?", Integer.class, team, alice);
		assertThat(count).isEqualTo(1);
	}

	@Test
	void rejectsTheSameMemberTwice() {
		UUID team = insertTeam("Platform", "platform");
		insertMember(team, alice);

		assertThatThrownBy(() -> insertMember(team, alice))
			.isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void rejectsAMemberWhoIsNotAUser() {
		UUID team = insertTeam("Platform", "platform");

		assertThatThrownBy(() -> insertMember(team, UUID.randomUUID()))
			.isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void rejectsAMemberOfATeamThatDoesNotExist() {
		assertThatThrownBy(() -> insertMember(UUID.randomUUID(), alice))
			.isInstanceOf(DataIntegrityViolationException.class);
	}

}
