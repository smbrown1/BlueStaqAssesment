package com.bluestaq.notes.note;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import com.bluestaq.notes.support.IntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

class ShareNoteTests extends IntegrationTest {

	private String noteId;
	private String teamId;

	@BeforeEach
	void aliceHasANoteAndBobHasATeam() throws Exception {
		registerUser("alice");
		registerUser("bob");
		noteId = createNote("alice", "Standup", "Shipped the migration.");
		teamId = createTeam("bob", "Platform");
	}

	private void share(String username, Object noteId, Object teamId) throws Exception {
		mockMvc.perform(put("/api/v1/notes/{id}/shares/{teamId}", noteId, teamId).with(as(username)))
			.andExpect(status().isNoContent())
			.andExpect(content().string(""));
	}

	@Test
	void theOwnerSharesWithATeamTheyDoNotBelongTo() throws Exception {
		share("alice", noteId, teamId);

		mockMvc.perform(get("/api/v1/notes/{id}", noteId).with(as("bob")))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.title").value("Standup"));

		mockMvc.perform(get("/api/v1/users/me/teams").with(as("alice")))
			.andExpect(jsonPath("$.content", hasSize(0)));
	}

	@Test
	void sharingAgainChangesNothing() throws Exception {
		share("alice", noteId, teamId);
		share("alice", noteId, teamId);

		mockMvc.perform(get("/api/v1/notes/{id}/shares", noteId).with(as("alice")))
			.andExpect(jsonPath("$", hasSize(1)));
	}

	@Test
	void anUnknownTeamGets404AndNamesTheTeam() throws Exception {
		mockMvc.perform(put("/api/v1/notes/{id}/shares/{teamId}", noteId, UUID.randomUUID()).with(as("alice")))
			.andExpect(status().isNotFound())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
			.andExpect(jsonPath("$.detail").value("Team not found."));
	}

	@Test
	void anUnknownNoteGets404() throws Exception {
		mockMvc.perform(put("/api/v1/notes/{id}/shares/{teamId}", UUID.randomUUID(), teamId).with(as("alice")))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.detail").value("Note not found."));
	}

	@Test
	void anIdThatIsNotAUuidGets400() throws Exception {
		mockMvc.perform(put("/api/v1/notes/{id}/shares/{teamId}", noteId, "not-a-uuid").with(as("alice")))
			.andExpect(status().isBadRequest());
	}

	@Test
	void needsCredentials() throws Exception {
		mockMvc.perform(put("/api/v1/notes/{id}/shares/{teamId}", noteId, teamId))
			.andExpect(status().isUnauthorized());
	}

	@Test
	void isDocumented() throws Exception {
		mockMvc.perform(get("/v3/api-docs"))
			.andExpect(jsonPath("$.paths['/api/v1/notes/{id}/shares/{teamId}'].put.summary").isNotEmpty())
			.andExpect(jsonPath("$.paths['/api/v1/notes/{id}/shares/{teamId}'].put.responses['204']").exists())
			.andExpect(jsonPath("$.paths['/api/v1/notes/{id}/shares/{teamId}'].put.responses['400']").exists())
			.andExpect(jsonPath("$.paths['/api/v1/notes/{id}/shares/{teamId}'].put.responses['401']").exists())
			.andExpect(jsonPath("$.paths['/api/v1/notes/{id}/shares/{teamId}'].put.responses['403']").exists())
			.andExpect(jsonPath("$.paths['/api/v1/notes/{id}/shares/{teamId}'].put.responses['404']").exists());
	}

}
