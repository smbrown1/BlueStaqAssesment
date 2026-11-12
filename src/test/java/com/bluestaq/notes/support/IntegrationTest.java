package com.bluestaq.notes.support;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/**
 * Base class for tests that call the API against the whole application and its database.
 *
 * <p>The tests are deliberately not wrapped in a transaction that rolls back. Each request
 * commits its own transaction, exactly as it does in the running service, so the tests see
 * the same flushing, constraint checks and version checks that a real caller would.
 * Isolation comes from emptying the tables before each test instead.
 */
@SpringBootTest
@AutoConfigureMockMvc
public abstract class IntegrationTest {

	/** The password of every user created with {@link #registerUser}. */
	protected static final String PASSWORD = "correct-horse";

	@Autowired
	protected MockMvc mockMvc;

	@Autowired
	protected JdbcTemplate jdbc;

	@BeforeEach
	void emptyTheDatabase() {
		// Tables that reference others go first, so no foreign key is broken.
		jdbc.update("delete from note_shares");
		jdbc.update("delete from team_members");
		jdbc.update("delete from teams");
		jdbc.update("delete from notes");
		jdbc.update("delete from users");
	}

	/** Registers a user with {@link #PASSWORD} and returns their id. */
	protected String registerUser(String username) throws Exception {
		String response = mockMvc.perform(post("/api/v1/users").contentType(MediaType.APPLICATION_JSON).content("""
			{"username": "%s", "password": "%s"}
			""".formatted(username, PASSWORD)))
			.andExpect(status().isCreated())
			.andReturn().getResponse().getContentAsString();
		return JsonPath.read(response, "$.id");
	}

	/** Creates a note as the given user and returns its id. */
	protected String createNote(String username, String title, String body) throws Exception {
		String response = mockMvc.perform(post("/api/v1/notes").with(as(username))
			.contentType(MediaType.APPLICATION_JSON).content("""
				{"title": "%s", "body": "%s"}
				""".formatted(title, body)))
			.andExpect(status().isCreated())
			.andReturn().getResponse().getContentAsString();
		return JsonPath.read(response, "$.id");
	}

	/** Creates a team as the given user, who becomes its first member, and returns its id. */
	protected String createTeam(String username, String name) throws Exception {
		String response = mockMvc.perform(post("/api/v1/teams").with(as(username))
			.contentType(MediaType.APPLICATION_JSON).content("""
				{"name": "%s"}
				""".formatted(name)))
			.andExpect(status().isCreated())
			.andReturn().getResponse().getContentAsString();
		return JsonPath.read(response, "$.id");
	}

	/** Sends the request with the HTTP Basic credentials of a user created with {@link #registerUser}. */
	protected static RequestPostProcessor as(String username) {
		return httpBasic(username, PASSWORD);
	}

}
