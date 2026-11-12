package com.bluestaq.notes.team;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.OffsetDateTime;
import java.util.UUID;

import com.bluestaq.notes.support.IntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.ResultActions;

class MyTeamsTests extends IntegrationTest {

	private String bobId;

	@BeforeEach
	void registerUsers() throws Exception {
		registerUser("alice");
		bobId = registerUser("bob");
	}

	private ResultActions myTeams(String username, String query) throws Exception {
		return mockMvc.perform(get("/api/v1/users/me/teams" + query).with(as(username)));
	}

	/** Puts a user in a team directly, so these tests do not depend on the add-member endpoint. */
	private void makeMember(String teamId, String userId) {
		jdbc.update("insert into team_members (team_id, user_id, added_at) values (?, ?, ?)",
			UUID.fromString(teamId), UUID.fromString(userId), OffsetDateTime.now());
	}

	@Test
	void listsOnlyTheTeamsTheCallerBelongsTo() throws Exception {
		String platform = createTeam("alice", "Platform");
		createTeam("bob", "Design");

		myTeams("alice", "")
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.content", hasSize(1)))
			.andExpect(jsonPath("$.content[0].id").value(platform))
			.andExpect(jsonPath("$.content[0].name").value("Platform"))
			.andExpect(jsonPath("$.page.totalElements").value(1));
	}

	@Test
	void includesTeamsTheCallerWasAddedTo() throws Exception {
		String platform = createTeam("alice", "Platform");
		createTeam("bob", "Design");
		makeMember(platform, bobId);

		myTeams("bob", "")
			.andExpect(jsonPath("$.content[*].name", containsInAnyOrder("Platform", "Design")));
	}

	@Test
	void eachItemHasOnlyTheIdAndName() throws Exception {
		createTeam("alice", "Platform");

		myTeams("alice", "")
			.andExpect(jsonPath("$.content[0].members").doesNotExist())
			.andExpect(jsonPath("$.content[0].length()").value(2));
	}

	@Test
	void sortsByNameIgnoringCase() throws Exception {
		createTeam("alice", "platform");
		createTeam("alice", "Design");
		createTeam("alice", "Ops");

		myTeams("alice", "")
			.andExpect(jsonPath("$.content[*].name", contains("Design", "Ops", "platform")));
	}

	@Test
	void aUserInNoTeamsGetsAnEmptyPage() throws Exception {
		createTeam("alice", "Platform");

		myTeams("bob", "")
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.content", hasSize(0)))
			.andExpect(jsonPath("$.page.totalElements").value(0));
	}

	@Test
	void isPagedLikeEveryOtherList() throws Exception {
		for (int i = 1; i <= 3; i++) {
			createTeam("alice", "Team " + i);
		}

		myTeams("alice", "?size=2&page=1")
			.andExpect(jsonPath("$.content", hasSize(1)))
			.andExpect(jsonPath("$.content[0].name").value("Team 3"))
			.andExpect(jsonPath("$.page.size").value(2))
			.andExpect(jsonPath("$.page.totalPages").value(2));
	}

	@Test
	void needsCredentials() throws Exception {
		mockMvc.perform(get("/api/v1/users/me/teams"))
			.andExpect(status().isUnauthorized());
	}

	@Test
	void isDocumented() throws Exception {
		mockMvc.perform(get("/v3/api-docs"))
			.andExpect(jsonPath("$.paths['/api/v1/users/me/teams'].get.summary").isNotEmpty())
			.andExpect(jsonPath("$.paths['/api/v1/users/me/teams'].get.parameters[*].name", containsInAnyOrder("page", "size")))
			.andExpect(jsonPath("$.paths['/api/v1/users/me/teams'].get.responses['200']").exists())
			.andExpect(jsonPath("$.paths['/api/v1/users/me/teams'].get.responses['401']").exists());
	}

}
