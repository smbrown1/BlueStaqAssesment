package com.bluestaq.notes.note;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
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

class UnshareNoteTests extends IntegrationTest {

	private String noteId;
	private String teamId;

	@BeforeEach
	void aliceSharesANoteWithBobsTeam() throws Exception {
		registerUser("alice");
		registerUser("bob");
		noteId = createNote("alice", "Standup", "Shipped the migration.");
		teamId = createTeam("bob", "Platform");
		mockMvc.perform(put("/api/v1/notes/{id}/shares/{teamId}", noteId, teamId).with(as("alice")))
			.andExpect(status().isNoContent());
	}

	private void unshare(String username, Object noteId, Object teamId) throws Exception {
		mockMvc.perform(delete("/api/v1/notes/{id}/shares/{teamId}", noteId, teamId).with(as(username)))
			.andExpect(status().isNoContent())
			.andExpect(content().string(""));
	}

	@Test
	void theOwnerStopsSharingAndMembersCanNoLongerReadIt() throws Exception {
		unshare("alice", noteId, teamId);

		mockMvc.perform(get("/api/v1/notes/{id}", noteId).with(as("bob")))
			.andExpect(status().isNotFound());
		mockMvc.perform(get("/api/v1/notes/{id}/shares", noteId).with(as("alice")))
			.andExpect(jsonPath("$", hasSize(0)));
	}

	@Test
	void stoppingAShareThatDoesNotExistChangesNothing() throws Exception {
		unshare("alice", noteId, UUID.randomUUID());

		mockMvc.perform(get("/api/v1/notes/{id}", noteId).with(as("bob")))
			.andExpect(status().isOk());
	}

	@Test
	void unsharingTwiceChangesNothing() throws Exception {
		unshare("alice", noteId, teamId);
		unshare("alice", noteId, teamId);
	}

	@Test
	void anUnknownNoteGets404() throws Exception {
		mockMvc.perform(delete("/api/v1/notes/{id}/shares/{teamId}", UUID.randomUUID(), teamId).with(as("alice")))
			.andExpect(status().isNotFound())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
			.andExpect(jsonPath("$.detail").value("Note not found."));
	}

	@Test
	void anIdThatIsNotAUuidGets400() throws Exception {
		mockMvc.perform(delete("/api/v1/notes/{id}/shares/{teamId}", "not-a-uuid", teamId).with(as("alice")))
			.andExpect(status().isBadRequest());
	}

	@Test
	void needsCredentials() throws Exception {
		mockMvc.perform(delete("/api/v1/notes/{id}/shares/{teamId}", noteId, teamId))
			.andExpect(status().isUnauthorized());

		mockMvc.perform(get("/api/v1/notes/{id}", noteId).with(as("bob")))
			.andExpect(status().isOk());
	}

	@Test
	void isDocumented() throws Exception {
		mockMvc.perform(get("/v3/api-docs"))
			.andExpect(jsonPath("$.paths['/api/v1/notes/{id}/shares/{teamId}'].delete.summary").isNotEmpty())
			.andExpect(jsonPath("$.paths['/api/v1/notes/{id}/shares/{teamId}'].delete.responses['204']").exists())
			.andExpect(jsonPath("$.paths['/api/v1/notes/{id}/shares/{teamId}'].delete.responses['400']").exists())
			.andExpect(jsonPath("$.paths['/api/v1/notes/{id}/shares/{teamId}'].delete.responses['401']").exists())
			.andExpect(jsonPath("$.paths['/api/v1/notes/{id}/shares/{teamId}'].delete.responses['403']").exists())
			.andExpect(jsonPath("$.paths['/api/v1/notes/{id}/shares/{teamId}'].delete.responses['404']").exists());
	}

}
