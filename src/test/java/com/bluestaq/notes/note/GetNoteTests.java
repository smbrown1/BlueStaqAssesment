package com.bluestaq.notes.note;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import com.bluestaq.notes.support.IntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

class GetNoteTests extends IntegrationTest {

	private String aliceId;
	private String noteId;

	@BeforeEach
	void aliceWritesANote() throws Exception {
		aliceId = registerUser("alice");
		registerUser("bob");
		noteId = createNote("alice", "Standup", "Shipped the migration.");
	}

	@Test
	void theOwnerCanReadTheirNote() throws Exception {
		mockMvc.perform(get("/api/v1/notes/{id}", noteId).with(as("alice")))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.id").value(noteId))
			.andExpect(jsonPath("$.title").value("Standup"))
			.andExpect(jsonPath("$.body").value("Shipped the migration."))
			.andExpect(jsonPath("$.version").value(0))
			.andExpect(jsonPath("$.owner.id").value(aliceId))
			.andExpect(jsonPath("$.owner.username").value("alice"))
			.andExpect(jsonPath("$.createdAt").isNotEmpty())
			.andExpect(jsonPath("$.updatedAt").isNotEmpty());
	}

	@Test
	void anotherUserGets404SoTheNoteStaysHidden() throws Exception {
		mockMvc.perform(get("/api/v1/notes/{id}", noteId).with(as("bob")))
			.andExpect(status().isNotFound())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
			.andExpect(jsonPath("$.status").value(404));
	}

	@Test
	void anUnknownIdGets404() throws Exception {
		mockMvc.perform(get("/api/v1/notes/{id}", UUID.randomUUID()).with(as("alice")))
			.andExpect(status().isNotFound())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
	}

	@Test
	void aHiddenNoteAndAMissingNoteLookTheSame() throws Exception {
		String hidden = mockMvc.perform(get("/api/v1/notes/{id}", noteId).with(as("bob")))
			.andReturn().getResponse().getContentAsString();
		UUID missingId = UUID.randomUUID();
		String missing = mockMvc.perform(get("/api/v1/notes/{id}", missingId).with(as("bob")))
			.andReturn().getResponse().getContentAsString();

		// Apart from the id in the path, nothing tells the caller which case they hit.
		org.assertj.core.api.Assertions.assertThat(hidden.replace(noteId, "ID"))
			.isEqualTo(missing.replace(missingId.toString(), "ID"));
	}

	@Test
	void anIdThatIsNotAUuidGets400() throws Exception {
		mockMvc.perform(get("/api/v1/notes/{id}", "not-a-uuid").with(as("alice")))
			.andExpect(status().isBadRequest());
	}

	@Test
	void needsCredentials() throws Exception {
		mockMvc.perform(get("/api/v1/notes/{id}", noteId))
			.andExpect(status().isUnauthorized());
	}

	@Test
	void isDocumented() throws Exception {
		mockMvc.perform(get("/v3/api-docs"))
			.andExpect(jsonPath("$.paths['/api/v1/notes/{id}'].get.summary").isNotEmpty())
			.andExpect(jsonPath("$.paths['/api/v1/notes/{id}'].get.responses['200']").exists())
			.andExpect(jsonPath("$.paths['/api/v1/notes/{id}'].get.responses['401']").exists())
			.andExpect(jsonPath("$.paths['/api/v1/notes/{id}'].get.responses['404']").exists());
	}

}
