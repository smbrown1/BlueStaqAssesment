package com.bluestaq.notes.user;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class RegisterUserValidationTests {

	private static final String VALID_PASSWORD = "correct-horse";

	@Autowired
	private MockMvc mockMvc;

	private ResultActions postJson(String body) throws Exception {
		return mockMvc.perform(post("/api/v1/users").contentType(MediaType.APPLICATION_JSON).content(body));
	}

	private ResultActions register(String username, String password) throws Exception {
		return postJson("""
			{"username": "%s", "password": "%s"}
			""".formatted(username, password));
	}

	@ParameterizedTest
	@ValueSource(strings = {
		"al",                                                    // shorter than 3
		"a123456789a123456789a123456789a123456789a123456789x",   // 51 characters
		"al ice",                                                // space
		"alice:smith",                                           // colon, which HTTP Basic cannot carry
		"alice@example.com",                                     // other symbols
	})
	void rejectsAnInvalidUsername(String username) throws Exception {
		register(username, VALID_PASSWORD)
			.andExpect(status().isBadRequest())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
			.andExpect(jsonPath("$.status").value(400))
			.andExpect(jsonPath("$.errors.username").isNotEmpty());
	}

	@ParameterizedTest
	@ValueSource(strings = {
		"a.b",
		"alice_smith-2",
		"a123456789a123456789a123456789a123456789a123456789",    // 50 characters
	})
	void acceptsAValidUsername(String username) throws Exception {
		register(username, VALID_PASSWORD).andExpect(status().isCreated());
	}

	@Test
	void rejectsAMissingUsername() throws Exception {
		postJson("""
			{"password": "correct-horse"}
			""")
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errors.username").isNotEmpty());
	}

	@Test
	void rejectsAPasswordShorterThan8Characters() throws Exception {
		register("alice", "1234567")
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errors.password").isNotEmpty());
	}

	@Test
	void acceptsPasswordsOf8And64Characters() throws Exception {
		register("alice", "12345678").andExpect(status().isCreated());
		register("bob", "x".repeat(64)).andExpect(status().isCreated());
	}

	@Test
	void rejectsAPasswordLongerThan64Characters() throws Exception {
		register("alice", "x".repeat(65))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errors.password").isNotEmpty());
	}

	@Test
	void rejectsAPasswordLongerThan72Bytes() throws Exception {
		// 40 characters, but each takes two bytes in UTF-8.
		register("alice", "é".repeat(40))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errors.password").isNotEmpty());
	}

	@Test
	void rejectsAMissingPassword() throws Exception {
		postJson("""
			{"username": "alice"}
			""")
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errors.password").isNotEmpty());
	}

	@Test
	void rejectsABodyThatIsNotJson() throws Exception {
		postJson("not json")
			.andExpect(status().isBadRequest())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
	}

	@Test
	void rejectsATakenUsername() throws Exception {
		register("alice", VALID_PASSWORD).andExpect(status().isCreated());

		register("alice", "another-password")
			.andExpect(status().isConflict())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
			.andExpect(jsonPath("$.status").value(409))
			.andExpect(jsonPath("$.detail").isNotEmpty());
	}

	@Test
	void treatsUsernamesThatDifferOnlyByCaseAsTaken() throws Exception {
		register("alice", VALID_PASSWORD).andExpect(status().isCreated());

		register("ALICE", VALID_PASSWORD).andExpect(status().isConflict());
	}

	@Test
	void documentsTheErrorResponses() throws Exception {
		mockMvc.perform(get("/v3/api-docs"))
			.andExpect(jsonPath("$.paths['/api/v1/users'].post.responses['400']").exists())
			.andExpect(jsonPath("$.paths['/api/v1/users'].post.responses['409']").exists());
	}

}
