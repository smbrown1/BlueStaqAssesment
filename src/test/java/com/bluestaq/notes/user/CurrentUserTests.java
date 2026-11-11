package com.bluestaq.notes.user;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CurrentUserTests {

	@Autowired
	private MockMvc mockMvc;

	/** Registers a user and returns their id. */
	private String register(String username) throws Exception {
		String response = mockMvc.perform(post("/api/v1/users").contentType(MediaType.APPLICATION_JSON).content("""
			{"username": "%s", "password": "correct-horse"}
			""".formatted(username)))
			.andExpect(status().isCreated())
			.andReturn().getResponse().getContentAsString();
		return JsonPath.read(response, "$.id");
	}

	@Test
	void returnsTheCallingUser() throws Exception {
		String aliceId = register("alice");
		register("bob");

		mockMvc.perform(get("/api/v1/users/me").with(httpBasic("alice", "correct-horse")))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.id").value(aliceId))
			.andExpect(jsonPath("$.username").value("alice"))
			.andExpect(jsonPath("$.createdAt").isNotEmpty())
			.andExpect(jsonPath("$.password").doesNotExist())
			.andExpect(jsonPath("$.passwordHash").doesNotExist());
	}

	@Test
	void needsCredentials() throws Exception {
		mockMvc.perform(get("/api/v1/users/me"))
			.andExpect(status().isUnauthorized());
	}

	@Test
	void isDocumentedAsNeedingCredentials() throws Exception {
		mockMvc.perform(get("/v3/api-docs"))
			.andExpect(jsonPath("$.paths['/api/v1/users/me'].get.summary").isNotEmpty())
			.andExpect(jsonPath("$.paths['/api/v1/users/me'].get.responses['200']").exists())
			.andExpect(jsonPath("$.paths['/api/v1/users/me'].get.responses['401']").exists())
			// No override, so the default HTTP Basic requirement applies.
			.andExpect(jsonPath("$.paths['/api/v1/users/me'].get.security").doesNotExist());
	}

}
