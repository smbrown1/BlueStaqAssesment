package com.bluestaq.notes.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

/**
 * HTTP Basic credentials are checked against registered users. The tests call a path
 * that has no endpoint: 404 means the caller got past authentication, 401 means they did not.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AuthenticationTests {

	private static final String PROTECTED_PATH = "/api/v1/no-such-endpoint";

	@Autowired
	private MockMvc mockMvc;

	@BeforeEach
	void registerAlice() throws Exception {
		mockMvc.perform(post("/api/v1/users").contentType(MediaType.APPLICATION_JSON).content("""
			{"username": "alice", "password": "correct-horse"}
			"""))
			.andExpect(status().isCreated());
	}

	@Test
	void acceptsARegisteredUsersCredentials() throws Exception {
		mockMvc.perform(get(PROTECTED_PATH).with(httpBasic("alice", "correct-horse")))
			.andExpect(status().isNotFound());
	}

	@Test
	void acceptsTheUsernameInAnyCase() throws Exception {
		mockMvc.perform(get(PROTECTED_PATH).with(httpBasic("ALICE", "correct-horse")))
			.andExpect(status().isNotFound());
	}

	@Test
	void rejectsAWrongPassword() throws Exception {
		mockMvc.perform(get(PROTECTED_PATH).with(httpBasic("alice", "wrong-password")))
			.andExpect(status().isUnauthorized());
	}

	@Test
	void rejectsAnUnknownUser() throws Exception {
		mockMvc.perform(get(PROTECTED_PATH).with(httpBasic("mallory", "correct-horse")))
			.andExpect(status().isUnauthorized());
	}

	@Test
	void rejectsARequestWithoutCredentialsAndAsksForBasic() throws Exception {
		mockMvc.perform(get(PROTECTED_PATH))
			.andExpect(status().isUnauthorized())
			.andExpect(header().string("WWW-Authenticate", org.hamcrest.Matchers.startsWith("Basic")));
	}

	@Test
	void doesNotCreateASession() throws Exception {
		MvcResult result = mockMvc.perform(get(PROTECTED_PATH).with(httpBasic("alice", "correct-horse")))
			.andReturn();

		assertThat(result.getRequest().getSession(false)).isNull();
	}

}
