package com.bluestaq.notes.note;

import static org.hamcrest.Matchers.contains;
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

class ListSharesTests extends IntegrationTest {

	private String noteId;
	private String platformId;

	@BeforeEach
	void aliceHasAPrivateNote() throws Exception {
		registerUser("alice");
		registerUser("bob");
		noteId = createNote("alice", "Standup", "Shipped the migration.");
		platformId = createTeam("bob", "Platform");
	}

	private void shareWith(String teamId) throws Exception {
		mockMvc.perform(put("/api/v1/notes/{id}/shares/{teamId}", noteId, teamId).with(as("alice")))
			.andExpect(status().isNoContent());
	}

	@Test
	void theOwnerSeesTheTeamsSortedByName() throws Exception {
		String designId = createTeam("alice", "design");
		shareWith(platformId);
		shareWith(designId);

		mockMvc.perform(get("/api/v1/notes/{id}/shares", noteId).with(as("alice")))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$", hasSize(2)))
			.andExpect(jsonPath("$[*].team.name", contains("design", "Platform")))
			.andExpect(jsonPath("$[1].team.id").value(platformId))
			.andExpect(jsonPath("$[1].sharedAt").isNotEmpty());
	}

	@Test
	void aPrivateNoteHasNoShares() throws Exception {
		mockMvc.perform(get("/api/v1/notes/{id}/shares", noteId).with(as("alice")))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$", hasSize(0)));
	}

	@Test
	void anUnknownNoteGets404() throws Exception {
		mockMvc.perform(get("/api/v1/notes/{id}/shares", UUID.randomUUID()).with(as("alice")))
			.andExpect(status().isNotFound())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
			.andExpect(jsonPath("$.detail").value("Note not found."));
	}

	@Test
	void needsCredentials() throws Exception {
		mockMvc.perform(get("/api/v1/notes/{id}/shares", noteId))
			.andExpect(status().isUnauthorized());
	}

	@Test
	void isDocumented() throws Exception {
		mockMvc.perform(get("/v3/api-docs"))
			.andExpect(jsonPath("$.paths['/api/v1/notes/{id}/shares'].get.summary").isNotEmpty())
			.andExpect(jsonPath("$.paths['/api/v1/notes/{id}/shares'].get.responses['200']").exists())
			.andExpect(jsonPath("$.paths['/api/v1/notes/{id}/shares'].get.responses['400']").exists())
			.andExpect(jsonPath("$.paths['/api/v1/notes/{id}/shares'].get.responses['401']").exists())
			.andExpect(jsonPath("$.paths['/api/v1/notes/{id}/shares'].get.responses['403']").exists())
			.andExpect(jsonPath("$.paths['/api/v1/notes/{id}/shares'].get.responses['404']").exists());
	}

}
