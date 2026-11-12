package com.bluestaq.notes.note;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import com.bluestaq.notes.support.IntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DeleteNoteTests extends IntegrationTest {

	private String noteId;

	@BeforeEach
	void aliceWritesANote() throws Exception {
		registerUser("alice");
		registerUser("bob");
		noteId = createNote("alice", "Standup", "Shipped the migration.");
	}

	private void assertAliceCanStillReadTheNote() throws Exception {
		mockMvc.perform(get("/api/v1/notes/{id}", noteId).with(as("alice")))
			.andExpect(status().isOk());
	}

	@Test
	void theOwnerDeletesTheirNote() throws Exception {
		mockMvc.perform(delete("/api/v1/notes/{id}", noteId).with(as("alice")))
			.andExpect(status().isNoContent())
			.andExpect(content().string(""));

		mockMvc.perform(get("/api/v1/notes/{id}", noteId).with(as("alice")))
			.andExpect(status().isNotFound());
	}

	@Test
	void deletingTwiceGets404TheSecondTime() throws Exception {
		mockMvc.perform(delete("/api/v1/notes/{id}", noteId).with(as("alice")))
			.andExpect(status().isNoContent());

		mockMvc.perform(delete("/api/v1/notes/{id}", noteId).with(as("alice")))
			.andExpect(status().isNotFound());
	}

	@Test
	void anotherUserGets404AndTheNoteSurvives() throws Exception {
		mockMvc.perform(delete("/api/v1/notes/{id}", noteId).with(as("bob")))
			.andExpect(status().isNotFound());

		assertAliceCanStillReadTheNote();
	}

	@Test
	void anUnknownIdGets404() throws Exception {
		mockMvc.perform(delete("/api/v1/notes/{id}", UUID.randomUUID()).with(as("alice")))
			.andExpect(status().isNotFound());
	}

	@Test
	void needsCredentials() throws Exception {
		mockMvc.perform(delete("/api/v1/notes/{id}", noteId))
			.andExpect(status().isUnauthorized());

		assertAliceCanStillReadTheNote();
	}

	@Test
	void isDocumented() throws Exception {
		mockMvc.perform(get("/v3/api-docs"))
			.andExpect(jsonPath("$.paths['/api/v1/notes/{id}'].delete.summary").isNotEmpty())
			.andExpect(jsonPath("$.paths['/api/v1/notes/{id}'].delete.responses['204']").exists())
			.andExpect(jsonPath("$.paths['/api/v1/notes/{id}'].delete.responses['401']").exists())
			.andExpect(jsonPath("$.paths['/api/v1/notes/{id}'].delete.responses['404']").exists());
	}

}
