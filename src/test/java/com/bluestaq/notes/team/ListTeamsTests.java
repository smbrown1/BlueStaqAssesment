package com.bluestaq.notes.team;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bluestaq.notes.support.IntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.ResultActions;

class ListTeamsTests extends IntegrationTest {

	@BeforeEach
	void registerUsers() throws Exception {
		registerUser("alice");
		registerUser("bob");
	}

	private ResultActions listAs(String username, String query) throws Exception {
		return mockMvc.perform(get("/api/v1/teams" + query).with(as(username)));
	}

	@Test
	void listsEveryTeamIncludingOnesTheCallerIsNotIn() throws Exception {
		createTeam("alice", "Platform");
		createTeam("bob", "Design");

		listAs("alice", "")
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.content[*].name", containsInAnyOrder("Platform", "Design")))
			.andExpect(jsonPath("$.page.totalElements").value(2));
	}

	@Test
	void eachItemHasOnlyTheIdAndName() throws Exception {
		String teamId = createTeam("alice", "Platform");

		listAs("bob", "")
			.andExpect(jsonPath("$.content[0].id").value(teamId))
			.andExpect(jsonPath("$.content[0].name").value("Platform"))
			.andExpect(jsonPath("$.content[0].members").doesNotExist())
			.andExpect(jsonPath("$.content[0].length()").value(2));
	}

	@Test
	void sortsByNameIgnoringCase() throws Exception {
		createTeam("alice", "platform");
		createTeam("alice", "Design");
		createTeam("alice", "Ops");

		listAs("alice", "")
			.andExpect(jsonPath("$.content[*].name", contains("Design", "Ops", "platform")));
	}

	@Test
	void anEmptyListIsAnEmptyPage() throws Exception {
		listAs("alice", "")
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.content", hasSize(0)))
			.andExpect(jsonPath("$.page.totalElements").value(0));
	}

	@Test
	void isPagedLikeEveryOtherList() throws Exception {
		for (int i = 1; i <= 3; i++) {
			createTeam("alice", "Team " + i);
		}

		listAs("alice", "")
			.andExpect(jsonPath("$.page.size").value(20))
			.andExpect(jsonPath("$.page.number").value(0));

		listAs("alice", "?size=2&page=1")
			.andExpect(jsonPath("$.content", hasSize(1)))
			.andExpect(jsonPath("$.content[0].name").value("Team 3"))
			.andExpect(jsonPath("$.page.totalPages").value(2));

		listAs("alice", "?size=500")
			.andExpect(jsonPath("$.page.size").value(100));
	}

	@Test
	void needsCredentials() throws Exception {
		mockMvc.perform(get("/api/v1/teams"))
			.andExpect(status().isUnauthorized());
	}

	@Test
	void isDocumented() throws Exception {
		mockMvc.perform(get("/v3/api-docs"))
			.andExpect(jsonPath("$.paths['/api/v1/teams'].get.summary").isNotEmpty())
			.andExpect(jsonPath("$.paths['/api/v1/teams'].get.parameters[*].name", containsInAnyOrder("page", "size")))
			.andExpect(jsonPath("$.paths['/api/v1/teams'].get.responses['200']").exists())
			.andExpect(jsonPath("$.paths['/api/v1/teams'].get.responses['401']").exists());
	}

}
