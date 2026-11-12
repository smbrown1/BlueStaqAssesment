package com.bluestaq.notes.note;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import com.bluestaq.notes.support.IntegrationTest;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

class UpdateNoteTests extends IntegrationTest {

	private String aliceId;
	private String noteId;

	@BeforeEach
	void aliceWritesANote() throws Exception {
		aliceId = registerUser("alice");
		registerUser("bob");
		noteId = createNote("alice", "Standup", "Shipped the migration.");
	}

	private ResultActions update(String username, Object id, String json) throws Exception {
		return mockMvc.perform(put("/api/v1/notes/{id}", id).with(as(username))
			.contentType(MediaType.APPLICATION_JSON).content(json));
	}

	private String readAsAlice() throws Exception {
		return mockMvc.perform(get("/api/v1/notes/{id}", noteId).with(as("alice")))
			.andExpect(status().isOk())
			.andReturn().getResponse().getContentAsString();
	}

	@Test
	void theOwnerReplacesTheTitleAndBody() throws Exception {
		update("alice", noteId, """
			{"title": "Standup, Tuesday", "body": "Rolled it back.", "version": 0}
			""")
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.id").value(noteId))
			.andExpect(jsonPath("$.title").value("Standup, Tuesday"))
			.andExpect(jsonPath("$.body").value("Rolled it back."))
			.andExpect(jsonPath("$.owner.id").value(aliceId));
	}

	@Test
	void anUpdateIsStoredAndMovesTheVersionAndUpdateTimeOn() throws Exception {
		String before = readAsAlice();

		update("alice", noteId, """
			{"title": "Standup, Tuesday", "body": "Rolled it back.", "version": 0}
			""")
			.andExpect(jsonPath("$.version").value(1));

		String after = readAsAlice();
		assertThat(JsonPath.<String>read(after, "$.title")).isEqualTo("Standup, Tuesday");
		assertThat(JsonPath.<Integer>read(after, "$.version")).isEqualTo(1);
		assertThat(JsonPath.<String>read(after, "$.createdAt")).isEqualTo(JsonPath.<String>read(before, "$.createdAt"));
		assertThat(JsonPath.<String>read(after, "$.updatedAt")).isNotEqualTo(JsonPath.<String>read(before, "$.updatedAt"));
	}

	@Test
	void aSecondUpdateFromTheSameStaleCopyIsRejected() throws Exception {
		// Two sessions both read version 0. The first save wins.
		update("alice", noteId, """
			{"title": "First save", "body": "", "version": 0}
			""")
			.andExpect(status().isOk());

		update("alice", noteId, """
			{"title": "Second save", "body": "", "version": 0}
			""")
			.andExpect(status().isConflict())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
			.andExpect(jsonPath("$.status").value(409))
			.andExpect(jsonPath("$.detail").isNotEmpty());

		assertThat(JsonPath.<String>read(readAsAlice(), "$.title")).isEqualTo("First save");
	}

	@Test
	void anUpdateCanFollowAnotherWhenItSendsTheNewVersion() throws Exception {
		update("alice", noteId, """
			{"title": "First save", "body": "", "version": 0}
			""")
			.andExpect(status().isOk());

		update("alice", noteId, """
			{"title": "Second save", "body": "", "version": 1}
			""")
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.version").value(2));
	}

	@Test
	void aVersionThatWasNeverIssuedIsRejected() throws Exception {
		update("alice", noteId, """
			{"title": "Guess", "body": "", "version": 7}
			""")
			.andExpect(status().isConflict());
	}

	@Test
	void rejectsAMissingVersion() throws Exception {
		update("alice", noteId, """
			{"title": "No version", "body": ""}
			""")
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errors.version").isNotEmpty());
	}

	@Test
	void appliesTheSameTitleAndBodyRulesAsCreation() throws Exception {
		update("alice", noteId, """
			{"title": " ", "body": "", "version": 0}
			""")
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errors.title").isNotEmpty());

		update("alice", noteId, """
			{"title": "Too long", "body": "%s", "version": 0}
			""".formatted("b".repeat(10_001)))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errors.body").isNotEmpty());
	}

	@Test
	void aMissingBodyEmptiesTheNote() throws Exception {
		update("alice", noteId, """
			{"title": "Title only", "version": 0}
			""")
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.body").value(""));
	}

	@Test
	void anotherUserGets404AndTheNoteIsUnchanged() throws Exception {
		update("bob", noteId, """
			{"title": "Hijacked", "body": "", "version": 0}
			""")
			.andExpect(status().isNotFound());

		assertThat(JsonPath.<String>read(readAsAlice(), "$.title")).isEqualTo("Standup");
	}

	@Test
	void anUnknownIdGets404() throws Exception {
		update("alice", UUID.randomUUID(), """
			{"title": "Nothing here", "body": "", "version": 0}
			""")
			.andExpect(status().isNotFound());
	}

	@Test
	void needsCredentials() throws Exception {
		mockMvc.perform(put("/api/v1/notes/{id}", noteId).contentType(MediaType.APPLICATION_JSON).content("""
			{"title": "Anonymous", "body": "", "version": 0}
			"""))
			.andExpect(status().isUnauthorized());
	}

	@Test
	void isDocumented() throws Exception {
		mockMvc.perform(get("/v3/api-docs"))
			.andExpect(jsonPath("$.paths['/api/v1/notes/{id}'].put.summary").isNotEmpty())
			.andExpect(jsonPath("$.paths['/api/v1/notes/{id}'].put.responses['200']").exists())
			.andExpect(jsonPath("$.paths['/api/v1/notes/{id}'].put.responses['400']").exists())
			.andExpect(jsonPath("$.paths['/api/v1/notes/{id}'].put.responses['401']").exists())
			.andExpect(jsonPath("$.paths['/api/v1/notes/{id}'].put.responses['404']").exists())
			.andExpect(jsonPath("$.paths['/api/v1/notes/{id}'].put.responses['409']").exists());
	}

}
