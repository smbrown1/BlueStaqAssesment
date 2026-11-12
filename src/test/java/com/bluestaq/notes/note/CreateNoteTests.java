package com.bluestaq.notes.note;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bluestaq.notes.support.IntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

class CreateNoteTests extends IntegrationTest {

	private String aliceId;

	@BeforeEach
	void registerAlice() throws Exception {
		aliceId = registerUser("alice");
	}

	private ResultActions createAsAlice(String json) throws Exception {
		return mockMvc.perform(post("/api/v1/notes").with(as("alice"))
			.contentType(MediaType.APPLICATION_JSON).content(json));
	}

	@Test
	void createsANoteOwnedByTheCaller() throws Exception {
		createAsAlice("""
			{"title": "Standup", "body": "Shipped the migration."}
			""")
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.id").isNotEmpty())
			.andExpect(jsonPath("$.title").value("Standup"))
			.andExpect(jsonPath("$.body").value("Shipped the migration."))
			.andExpect(jsonPath("$.version").value(0))
			.andExpect(jsonPath("$.owner.id").value(aliceId))
			.andExpect(jsonPath("$.owner.username").value("alice"))
			.andExpect(jsonPath("$.createdAt").isNotEmpty())
			.andExpect(jsonPath("$.updatedAt").isNotEmpty());
	}

	@Test
	void storesTheNote() throws Exception {
		createAsAlice("""
			{"title": "Standup", "body": "Shipped the migration."}
			""");

		Integer count = jdbc.queryForObject(
			"select count(*) from notes where title = 'Standup' and owner_id = ?", Integer.class,
			java.util.UUID.fromString(aliceId));
		org.assertj.core.api.Assertions.assertThat(count).isEqualTo(1);
	}

	@Test
	void treatsAMissingBodyAsEmpty() throws Exception {
		createAsAlice("""
			{"title": "Title only"}
			""")
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.body").value(""));
	}

	@Test
	void acceptsTheLongestTitleAndBody() throws Exception {
		createAsAlice("""
			{"title": "%s", "body": "%s"}
			""".formatted("t".repeat(200), "b".repeat(10_000)))
			.andExpect(status().isCreated());
	}

	@Test
	void rejectsAMissingTitle() throws Exception {
		createAsAlice("""
			{"body": "No title"}
			""")
			.andExpect(status().isBadRequest())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
			.andExpect(jsonPath("$.errors.title").isNotEmpty());
	}

	@Test
	void rejectsABlankTitle() throws Exception {
		createAsAlice("""
			{"title": "   ", "body": "Blank title"}
			""")
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errors.title").isNotEmpty());
	}

	@Test
	void rejectsATitleLongerThan200Characters() throws Exception {
		createAsAlice("""
			{"title": "%s"}
			""".formatted("t".repeat(201)))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errors.title").isNotEmpty());
	}

	@Test
	void rejectsABodyLongerThan10000Characters() throws Exception {
		createAsAlice("""
			{"title": "Too long", "body": "%s"}
			""".formatted("b".repeat(10_001)))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errors.body").isNotEmpty());
	}

	@Test
	void needsCredentials() throws Exception {
		mockMvc.perform(post("/api/v1/notes").contentType(MediaType.APPLICATION_JSON).content("""
			{"title": "Standup"}
			"""))
			.andExpect(status().isUnauthorized());
	}

	@Test
	void isDocumented() throws Exception {
		mockMvc.perform(get("/v3/api-docs"))
			.andExpect(jsonPath("$.paths['/api/v1/notes'].post.summary").isNotEmpty())
			.andExpect(jsonPath("$.paths['/api/v1/notes'].post.responses['201']").exists())
			.andExpect(jsonPath("$.paths['/api/v1/notes'].post.responses['400']").exists())
			.andExpect(jsonPath("$.paths['/api/v1/notes'].post.responses['401']").exists());
	}

}
