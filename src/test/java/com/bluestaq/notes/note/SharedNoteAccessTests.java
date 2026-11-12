package com.bluestaq.notes.note;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bluestaq.notes.support.IntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

/** A team member may read a shared note, and only the owner may change it (decision 017). */
class SharedNoteAccessTests extends IntegrationTest {

	private String noteId;
	private String teamId;

	@BeforeEach
	void aliceSharesANoteWithBobsTeam() throws Exception {
		registerUser("alice");
		registerUser("bob");
		registerUser("carol");
		noteId = createNote("alice", "Standup", "Shipped the migration.");
		teamId = createTeam("bob", "Platform");
		mockMvc.perform(put("/api/v1/notes/{id}/shares/{teamId}", noteId, teamId).with(as("alice")))
			.andExpect(status().isNoContent());
	}

	@Test
	void aTeamMemberCanReadTheNote() throws Exception {
		mockMvc.perform(get("/api/v1/notes/{id}", noteId).with(as("bob")))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.title").value("Standup"))
			.andExpect(jsonPath("$.owner.username").value("alice"));
	}

	@Test
	void someoneOutsideTheTeamStillGets404() throws Exception {
		mockMvc.perform(get("/api/v1/notes/{id}", noteId).with(as("carol")))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.detail").value("Note not found."));
	}

	@Test
	void aTeamMemberCannotReplaceTheNote() throws Exception {
		mockMvc.perform(put("/api/v1/notes/{id}", noteId).with(as("bob"))
			.contentType(MediaType.APPLICATION_JSON).content("""
				{"title": "Taken over", "body": "", "version": 0}
				"""))
			.andExpect(status().isForbidden())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
			.andExpect(jsonPath("$.detail").value("Only the note's owner can change or share it."));

		mockMvc.perform(get("/api/v1/notes/{id}", noteId).with(as("alice")))
			.andExpect(jsonPath("$.title").value("Standup"))
			.andExpect(jsonPath("$.version").value(0));
	}

	@Test
	void aTeamMemberCannotDeleteTheNote() throws Exception {
		mockMvc.perform(delete("/api/v1/notes/{id}", noteId).with(as("bob")))
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.detail").value("Only the note's owner can change or share it."));

		mockMvc.perform(get("/api/v1/notes/{id}", noteId).with(as("alice")))
			.andExpect(status().isOk());
	}

	@Test
	void aTeamMemberCannotShareOrUnshareTheNote() throws Exception {
		String otherTeam = createTeam("carol", "Design");

		mockMvc.perform(put("/api/v1/notes/{id}/shares/{teamId}", noteId, otherTeam).with(as("bob")))
			.andExpect(status().isForbidden());
		mockMvc.perform(delete("/api/v1/notes/{id}/shares/{teamId}", noteId, teamId).with(as("bob")))
			.andExpect(status().isForbidden());
		mockMvc.perform(get("/api/v1/notes/{id}/shares", noteId).with(as("bob")))
			.andExpect(status().isForbidden());

		mockMvc.perform(get("/api/v1/notes/{id}", noteId).with(as("bob")))
			.andExpect(status().isOk());
	}

	@Test
	void changeEndpointsDocumentThe403() throws Exception {
		mockMvc.perform(get("/v3/api-docs"))
			.andExpect(jsonPath("$.paths['/api/v1/notes/{id}'].put.responses['403']").exists())
			.andExpect(jsonPath("$.paths['/api/v1/notes/{id}'].delete.responses['403']").exists());
	}

}
