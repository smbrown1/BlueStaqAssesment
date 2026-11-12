package com.bluestaq.notes.team;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
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
import org.springframework.test.web.servlet.ResultActions;

class AddMemberTests extends IntegrationTest {

	private String teamId;

	@BeforeEach
	void aliceCreatesATeam() throws Exception {
		registerUser("alice");
		registerUser("bob");
		registerUser("carol");
		teamId = createTeam("alice", "Platform");
	}

	private ResultActions add(String username, Object teamId, String json) throws Exception {
		return mockMvc.perform(post("/api/v1/teams/{id}/members", teamId).with(as(username))
			.contentType(MediaType.APPLICATION_JSON).content(json));
	}

	private ResultActions addNamed(String caller, String newMember) throws Exception {
		return add(caller, teamId, """
			{"username": "%s"}
			""".formatted(newMember));
	}

	@Test
	void aMemberAddsAUserAndGetsTheTeamBack() throws Exception {
		addNamed("alice", "bob")
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.id").value(teamId))
			.andExpect(jsonPath("$.members[*].username", contains("alice", "bob")));

		mockMvc.perform(get("/api/v1/teams/{id}", teamId).with(as("bob")))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.members[*].username", contains("alice", "bob")));
	}

	@Test
	void matchesTheUsernameIgnoringCaseAndOuterSpaces() throws Exception {
		addNamed("alice", "  BoB  ")
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.members[*].username", contains("alice", "bob")));
	}

	@Test
	void addingSomeoneWhoIsAlreadyAMemberSucceeds() throws Exception {
		addNamed("alice", "bob").andExpect(status().isOk());

		addNamed("alice", "bob")
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.members", hasSize(2)));
	}

	@Test
	void anUnknownUsernameGets404() throws Exception {
		addNamed("alice", "nobody")
			.andExpect(status().isNotFound())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
			.andExpect(jsonPath("$.status").value(404))
			.andExpect(jsonPath("$.detail").value("No user with the username 'nobody'."));
	}

	@Test
	void someoneWhoIsNotAMemberGets403AndTheTeamIsUnchanged() throws Exception {
		addNamed("bob", "carol")
			.andExpect(status().isForbidden())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
			.andExpect(jsonPath("$.detail").value("You are not a member of this team."));

		mockMvc.perform(get("/api/v1/teams/{id}", teamId).with(as("alice")))
			.andExpect(jsonPath("$.members", hasSize(1)));
	}

	@Test
	void anUnknownTeamGets404() throws Exception {
		add("alice", UUID.randomUUID(), """
			{"username": "bob"}
			""")
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.detail").value("Team not found."));
	}

	@Test
	void aBlankUsernameGets400() throws Exception {
		addNamed("alice", "   ")
			.andExpect(status().isBadRequest())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
			.andExpect(jsonPath("$.errors.username").isNotEmpty());
	}

	@Test
	void anIdThatIsNotAUuidGets400() throws Exception {
		add("alice", "not-a-uuid", """
			{"username": "bob"}
			""")
			.andExpect(status().isBadRequest());
	}

	@Test
	void needsCredentials() throws Exception {
		mockMvc.perform(post("/api/v1/teams/{id}/members", teamId)
			.contentType(MediaType.APPLICATION_JSON).content("""
				{"username": "bob"}
				"""))
			.andExpect(status().isUnauthorized());
	}

	@Test
	void isDocumented() throws Exception {
		mockMvc.perform(get("/v3/api-docs"))
			.andExpect(jsonPath("$.paths['/api/v1/teams/{id}/members'].post.summary").isNotEmpty())
			.andExpect(jsonPath("$.paths['/api/v1/teams/{id}/members'].post.responses['200']").exists())
			.andExpect(jsonPath("$.paths['/api/v1/teams/{id}/members'].post.responses['400']").exists())
			.andExpect(jsonPath("$.paths['/api/v1/teams/{id}/members'].post.responses['401']").exists())
			.andExpect(jsonPath("$.paths['/api/v1/teams/{id}/members'].post.responses['403']").exists())
			.andExpect(jsonPath("$.paths['/api/v1/teams/{id}/members'].post.responses['404']").exists());
	}

}
