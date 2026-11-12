package com.bluestaq.notes.team;

import static org.hamcrest.Matchers.contains;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import com.bluestaq.notes.support.IntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

class GetTeamTests extends IntegrationTest {

	private String aliceId;
	private String teamId;

	@BeforeEach
	void aliceCreatesATeam() throws Exception {
		aliceId = registerUser("alice");
		registerUser("bob");
		teamId = createTeam("alice", "Platform");
	}

	@Test
	void aMemberSeesTheTeamAndItsMembers() throws Exception {
		mockMvc.perform(post("/api/v1/teams/{id}/members", teamId).with(as("alice"))
			.contentType(MediaType.APPLICATION_JSON).content("""
				{"username": "bob"}
				"""))
			.andExpect(status().isOk());

		mockMvc.perform(get("/api/v1/teams/{id}", teamId).with(as("bob")))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.id").value(teamId))
			.andExpect(jsonPath("$.name").value("Platform"))
			.andExpect(jsonPath("$.createdAt").isNotEmpty())
			.andExpect(jsonPath("$.members[*].username", contains("alice", "bob")))
			.andExpect(jsonPath("$.members[0].id").value(aliceId));
	}

	@Test
	void someoneWhoIsNotAMemberGets403() throws Exception {
		mockMvc.perform(get("/api/v1/teams/{id}", teamId).with(as("bob")))
			.andExpect(status().isForbidden())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
			.andExpect(jsonPath("$.status").value(403))
			.andExpect(jsonPath("$.detail").value("You are not a member of this team."));
	}

	@Test
	void anUnknownIdGets404() throws Exception {
		mockMvc.perform(get("/api/v1/teams/{id}", UUID.randomUUID()).with(as("alice")))
			.andExpect(status().isNotFound())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
			.andExpect(jsonPath("$.detail").value("Team not found."));
	}

	@Test
	void anIdThatIsNotAUuidGets400() throws Exception {
		mockMvc.perform(get("/api/v1/teams/{id}", "not-a-uuid").with(as("alice")))
			.andExpect(status().isBadRequest());
	}

	@Test
	void needsCredentials() throws Exception {
		mockMvc.perform(get("/api/v1/teams/{id}", teamId))
			.andExpect(status().isUnauthorized());
	}

	@Test
	void isDocumented() throws Exception {
		mockMvc.perform(get("/v3/api-docs"))
			.andExpect(jsonPath("$.paths['/api/v1/teams/{id}'].get.summary").isNotEmpty())
			.andExpect(jsonPath("$.paths['/api/v1/teams/{id}'].get.responses['200']").exists())
			.andExpect(jsonPath("$.paths['/api/v1/teams/{id}'].get.responses['400']").exists())
			.andExpect(jsonPath("$.paths['/api/v1/teams/{id}'].get.responses['401']").exists())
			.andExpect(jsonPath("$.paths['/api/v1/teams/{id}'].get.responses['403']").exists())
			.andExpect(jsonPath("$.paths['/api/v1/teams/{id}'].get.responses['404']").exists());
	}

}
