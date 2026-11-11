package com.bluestaq.notes;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class HealthEndpointTests {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void healthIsUpWithoutCredentials() throws Exception {
		mockMvc.perform(get("/actuator/health"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value("UP"));
	}

	@Test
	void healthReportsTheDatabaseConnection() throws Exception {
		mockMvc.perform(get("/actuator/health"))
			.andExpect(jsonPath("$.components.db.status").value("UP"));
	}

	@Test
	void otherPathsStillNeedCredentials() throws Exception {
		mockMvc.perform(get("/api/v1/notes"))
			.andExpect(status().isUnauthorized());
	}

}
