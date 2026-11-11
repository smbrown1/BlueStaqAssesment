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
class ApiDocsTests {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void openApiDescriptionIsServedWithoutCredentials() throws Exception {
		mockMvc.perform(get("/v3/api-docs"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.openapi").exists())
			.andExpect(jsonPath("$.info.title").value("Notes API"));
	}

	@Test
	void swaggerUiPageIsServedWithoutCredentials() throws Exception {
		mockMvc.perform(get("/swagger-ui/index.html"))
			.andExpect(status().isOk());
	}

	@Test
	void openApiDescriptionDeclaresHttpBasic() throws Exception {
		mockMvc.perform(get("/v3/api-docs"))
			.andExpect(jsonPath("$.components.securitySchemes.basicAuth.type").value("http"))
			.andExpect(jsonPath("$.components.securitySchemes.basicAuth.scheme").value("basic"))
			.andExpect(jsonPath("$.security[0].basicAuth").exists());
	}

}
