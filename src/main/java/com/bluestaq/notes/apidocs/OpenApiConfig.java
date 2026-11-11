package com.bluestaq.notes.apidocs;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

	/** Name of the HTTP Basic scheme in the OpenAPI description. */
	public static final String BASIC_AUTH = "basicAuth";

	/**
	 * Describes the API for Swagger UI. HTTP Basic is required by default, which gives
	 * the page its Authorize button. An endpoint that needs no credentials opts out
	 * with an empty {@code @SecurityRequirements} annotation.
	 */
	@Bean
	OpenAPI notesOpenApi() {
		return new OpenAPI()
			.info(new Info()
				.title("Notes API")
				.version("v1")
				.description("Note-taking service for small teams. A user owns their notes and shares them with teams."))
			.components(new Components()
				.addSecuritySchemes(BASIC_AUTH, new SecurityScheme()
					.type(SecurityScheme.Type.HTTP)
					.scheme("basic")))
			.addSecurityItem(new SecurityRequirement().addList(BASIC_AUTH));
	}

}
