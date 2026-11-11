package com.bluestaq.notes.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class RegisterUserTests {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JdbcTemplate jdbc;

	@Autowired
	private EntityManager entityManager;

	private ResultActions register(String username, String password) throws Exception {
		String body = """
			{"username": "%s", "password": "%s"}
			""".formatted(username, password);
		return mockMvc.perform(post("/api/v1/users").contentType(MediaType.APPLICATION_JSON).content(body));
	}

	@Test
	void registersAUserWithoutCredentials() throws Exception {
		register("alice", "correct-horse")
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.id").isNotEmpty())
			.andExpect(jsonPath("$.username").value("alice"))
			.andExpect(jsonPath("$.createdAt").isNotEmpty());
	}

	@Test
	void neverReturnsThePassword() throws Exception {
		register("alice", "correct-horse")
			.andExpect(jsonPath("$.password").doesNotExist())
			.andExpect(jsonPath("$.passwordHash").doesNotExist());
	}

	@Test
	void storesThePasswordAsABcryptHash() throws Exception {
		register("alice", "correct-horse");
		// The test shares one transaction with the request, so push the pending insert to the database.
		entityManager.flush();

		String stored = jdbc.queryForObject(
			"select password_hash from users where username = ?", String.class, "alice");
		assertThat(stored).startsWith("$2").doesNotContain("correct-horse");
	}

	@Test
	void storesTheUsernameInLowerCase() throws Exception {
		register("Alice", "correct-horse")
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.username").value("alice"));
	}

	@Test
	void isDocumentedAsAnOpenEndpoint() throws Exception {
		mockMvc.perform(get("/v3/api-docs"))
			.andExpect(jsonPath("$.paths['/api/v1/users'].post.summary").isNotEmpty())
			.andExpect(jsonPath("$.paths['/api/v1/users'].post.responses['201']").exists())
			.andExpect(jsonPath("$.paths['/api/v1/users'].post.security").isEmpty());
	}

}
