package com.bluestaq.notes.user;

import com.bluestaq.notes.security.AuthenticatedUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
@Tag(name = "Users")
public class UserController {

	private final UserService userService;

	public UserController(UserService userService) {
		this.userService = userService;
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	@Operation(summary = "Register a new user", description = "Open to anyone; no credentials needed.")
	@SecurityRequirements
	@ApiResponse(responseCode = "201", description = "The user was registered.")
	@ApiResponse(responseCode = "400", description = "The username or password breaks the rules, or the body is not valid JSON.",
		content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ProblemDetail.class)))
	@ApiResponse(responseCode = "409", description = "The username is already taken.",
		content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ProblemDetail.class)))
	public UserResponse register(@Valid @RequestBody RegisterUserRequest request) {
		return UserResponse.from(userService.register(request.username(), request.password()));
	}

	@GetMapping("/me")
	@Operation(summary = "Get the calling user", description = "Also a simple way to check that a username and password work.")
	@ApiResponse(responseCode = "200", description = "The user whose credentials were sent.")
	@ApiResponse(responseCode = "401", description = "The credentials are missing or wrong.", content = @Content)
	public UserResponse me(@Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser caller) {
		return UserResponse.from(userService.get(caller.id()));
	}

}
