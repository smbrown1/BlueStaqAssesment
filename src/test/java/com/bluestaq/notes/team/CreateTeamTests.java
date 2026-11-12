package com.bluestaq.notes.team;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bluestaq.notes.support.IntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

class CreateTeamTests extends IntegrationTest {

	private String aliceId;

	@BeforeEach
	void registerUsers() throws Exception {
		aliceId = registerUser("alice");
		registerUser("bob");
	}

	private ResultActions create(String username, String json) throws Exception {
		return mockMvc.perform(post("/api/v1/teams").with(as(username))
			.contentType(MediaType.APPLICATION_JSON).content(json));
	}

	private ResultActions createNamed(String username, String name) throws Exception {
		return create(username, """
			{"name": "%s"}
			""".formatted(name));
	}

	@Test
	void createsATeamWithTheCallerAsItsFirstMember() throws Exception {
		createNamed("alice", "Platform Team")
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.id").isNotEmpty())
			.andExpect(jsonPath("$.name").value("Platform Team"))
			.andExpect(jsonPath("$.createdAt").isNotEmpty())
			.andExpect(jsonPath("$.members", hasSize(1)))
			.andExpect(jsonPath("$.members[0].id").value(aliceId))
			.andExpect(jsonPath("$.members[0].username").value("alice"))
			.andExpect(jsonPath("$.members[0].addedAt").isNotEmpty());
	}

	@Test
	void removesSpacesAtEitherEndOfTheName() throws Exception {
		createNamed("alice", "  Platform Team  ")
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.name").value("Platform Team"));
	}

	@Test
	void rejectsATakenName() throws Exception {
		createNamed("alice", "Platform").andExpect(status().isCreated());

		createNamed("bob", "Platform")
			.andExpect(status().isConflict())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
			.andExpect(jsonPath("$.status").value(409))
			.andExpect(jsonPath("$.detail").isNotEmpty());
	}

	@Test
	void treatsNamesThatDifferOnlyByCaseOrOuterSpacesAsTaken() throws Exception {
		createNamed("alice", "Platform").andExpect(status().isCreated());

		createNamed("bob", "PLATFORM").andExpect(status().isConflict());
		createNamed("bob", " platform ").andExpect(status().isConflict());
	}

	@ParameterizedTest
	@ValueSource(strings = {
		"ab",                                                    // shorter than 3
		"  ab  ",                                                // 2 once trimmed
		"   ",                                                   // blank
		"a123456789a123456789a123456789a123456789a123456789x",   // 51 characters
	})
	void rejectsAnInvalidName(String name) throws Exception {
		createNamed("alice", name)
			.andExpect(status().isBadRequest())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
			.andExpect(jsonPath("$.errors.name").isNotEmpty());
	}

	@ParameterizedTest
	@ValueSource(strings = {
		"Ops",
		"a123456789a123456789a123456789a123456789a123456789",         // 50 characters
		"  a123456789a123456789a123456789a123456789a123456789  ",     // 50 once trimmed
	})
	void acceptsAValidName(String name) throws Exception {
		createNamed("alice", name).andExpect(status().isCreated());
	}

	@Test
	void rejectsAMissingName() throws Exception {
		create("alice", "{}")
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errors.name").isNotEmpty());
	}

	@Test
	void needsCredentials() throws Exception {
		mockMvc.perform(post("/api/v1/teams").contentType(MediaType.APPLICATION_JSON).content("""
			{"name": "Platform"}
			"""))
			.andExpect(status().isUnauthorized());
	}

	@Test
	void isDocumented() throws Exception {
		mockMvc.perform(get("/v3/api-docs"))
			.andExpect(jsonPath("$.paths['/api/v1/teams'].post.summary").isNotEmpty())
			.andExpect(jsonPath("$.paths['/api/v1/teams'].post.responses['201']").exists())
			.andExpect(jsonPath("$.paths['/api/v1/teams'].post.responses['400']").exists())
			.andExpect(jsonPath("$.paths['/api/v1/teams'].post.responses['401']").exists())
			.andExpect(jsonPath("$.paths['/api/v1/teams'].post.responses['409']").exists());
	}

}
