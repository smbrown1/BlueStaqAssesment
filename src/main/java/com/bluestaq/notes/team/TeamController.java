package com.bluestaq.notes.team;

import com.bluestaq.notes.paging.Paging;
import com.bluestaq.notes.security.AuthenticatedUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Teams")
@ApiResponse(responseCode = "401", description = "The credentials are missing or wrong.", content = @Content)
public class TeamController {

	private final TeamService teamService;

	public TeamController(TeamService teamService) {
		this.teamService = teamService;
	}

	@PostMapping("/teams")
	@ResponseStatus(HttpStatus.CREATED)
	@Operation(summary = "Create a team", description = "The caller becomes the team's first member.")
	@ApiResponse(responseCode = "201", description = "The team was created.")
	@ApiResponse(responseCode = "400", description = "The name breaks the rules, or the body is not valid JSON.",
		content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ProblemDetail.class)))
	@ApiResponse(responseCode = "409", description = "A team with that name, ignoring case, already exists.",
		content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ProblemDetail.class)))
	public TeamResponse create(@Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser caller,
			@Valid @RequestBody CreateTeamRequest request) {
		return teamService.create(caller.id(), request.name());
	}

	@GetMapping("/teams")
	@Operation(summary = "List all teams",
		description = "Every team's id and name, sorted by name, whether or not the caller is a member. "
			+ "Use it to find a team to share a note with.")
	@ApiResponse(responseCode = "200", description = "One page of teams.")
	@ApiResponse(responseCode = "400", description = "The page or size is not a number.",
		content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ProblemDetail.class)))
	public Page<TeamSummary> listAll(
			@Parameter(description = "Page number, starting at 0.")
			@RequestParam(defaultValue = "0") int page,
			@Parameter(description = "Teams per page. Values above 100 are treated as 100.")
			@RequestParam(defaultValue = Paging.DEFAULT_SIZE) int size) {
		return teamService.listAll(page, size);
	}

}
