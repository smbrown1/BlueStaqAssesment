package com.bluestaq.notes.user;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bluestaq.notes.support.IntegrationTest;
import org.junit.jupiter.api.Test;

class CurrentUserTests extends IntegrationTest {

	@Test
	void returnsTheCallingUser() throws Exception {
		String aliceId = registerUser("alice");
		registerUser("bob");

		mockMvc.perform(get("/api/v1/users/me").with(as("alice")))
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
