package com.bluestaq.notes.note;

import java.util.List;
import java.util.UUID;

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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/notes")
@Tag(name = "Notes")
@ApiResponse(responseCode = "401", description = "The credentials are missing or wrong.", content = @Content)
public class NoteController {

	private static final String NOT_FOUND = "There is no such note, or the caller is not allowed to see it.";
	private static final String NOT_OWNER = "The caller can read the note but does not own it.";

	private final NoteService noteService;

	public NoteController(NoteService noteService) {
		this.noteService = noteService;
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	@Operation(summary = "Create a note", description = "The note belongs to the caller and is private until shared.")
	@ApiResponse(responseCode = "201", description = "The note was created.")
	@ApiResponse(responseCode = "400", description = "The title or body breaks the rules, or the body is not valid JSON.",
		content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ProblemDetail.class)))
	public NoteResponse create(@Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser caller,
			@Valid @RequestBody CreateNoteRequest request) {
		return noteService.create(caller.id(), request.title(), request.body());
	}

	@GetMapping
	@Operation(summary = "List notes the caller can read",
		description = "The caller's own notes and the notes shared with their teams, most recently updated first. "
			+ "The order is fixed. Narrow the list with scope, or with teamId to see what is shared with one team.")
	@ApiResponse(responseCode = "200", description = "One page of notes.")
	@ApiResponse(responseCode = "400", description = "The page or size is not a number, the scope is not recognised, or teamId is not a UUID.",
		content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ProblemDetail.class)))
	@ApiResponse(responseCode = "403", description = "teamId names a team the caller is not a member of.",
		content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ProblemDetail.class)))
	@ApiResponse(responseCode = "404", description = "teamId names a team that does not exist.",
		content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ProblemDetail.class)))
	public Page<NoteResponse> list(@Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser caller,
			@Parameter(description = "all: own notes and notes shared with the caller's teams. owned: own notes only. "
				+ "shared: other people's notes shared with the caller's teams. Ignored when teamId is given.",
				schema = @Schema(allowableValues = { "all", "owned", "shared" }, defaultValue = "all"))
			@RequestParam(defaultValue = "all") String scope,
			@Parameter(description = "List only the notes shared with this team. The caller must be a member of it.")
			@RequestParam(required = false) UUID teamId,
			@Parameter(description = "Page number, starting at 0.")
			@RequestParam(defaultValue = "0") int page,
			@Parameter(description = "Notes per page. Values above 100 are treated as 100.")
			@RequestParam(defaultValue = Paging.DEFAULT_SIZE) int size) {
		return noteService.list(caller.id(), NoteScope.parse(scope), teamId, page, size);
	}

	@GetMapping("/{id}")
	@Operation(summary = "Read a note", description = "Allowed for the owner and for members of a team the note is shared with.")
	@ApiResponse(responseCode = "200", description = "The note.")
	@ApiResponse(responseCode = "400", description = "The id is not a UUID.",
		content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ProblemDetail.class)))
	@ApiResponse(responseCode = "404", description = NOT_FOUND,
		content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ProblemDetail.class)))
	public NoteResponse get(@Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser caller,
			@PathVariable UUID id) {
		return noteService.get(caller.id(), id);
	}

	@PutMapping("/{id}")
	@Operation(summary = "Replace a note's title and body",
		description = "Owner only. Send the version the note had when you read it. If the note has been saved since, "
			+ "the update is rejected with 409 rather than overwriting that change.")
	@ApiResponse(responseCode = "200", description = "The updated note, with its new version.")
	@ApiResponse(responseCode = "400", description = "The title, body or version breaks the rules, or the id is not a UUID.",
		content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ProblemDetail.class)))
	@ApiResponse(responseCode = "403", description = NOT_OWNER,
		content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ProblemDetail.class)))
	@ApiResponse(responseCode = "404", description = NOT_FOUND,
		content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ProblemDetail.class)))
	@ApiResponse(responseCode = "409", description = "The note has been saved since the version that was sent.",
		content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ProblemDetail.class)))
	public NoteResponse update(@Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser caller,
			@PathVariable UUID id, @Valid @RequestBody UpdateNoteRequest request) {
		return noteService.update(caller.id(), id, request.title(), request.body(), request.version());
	}

	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@Operation(summary = "Delete a note", description = "Owner only. The note's shares are removed with it.")
	@ApiResponse(responseCode = "204", description = "The note was deleted.")
	@ApiResponse(responseCode = "400", description = "The id is not a UUID.",
		content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ProblemDetail.class)))
	@ApiResponse(responseCode = "403", description = NOT_OWNER,
		content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ProblemDetail.class)))
	@ApiResponse(responseCode = "404", description = NOT_FOUND,
		content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ProblemDetail.class)))
	public void delete(@Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser caller,
			@PathVariable UUID id) {
		noteService.delete(caller.id(), id);
	}

	@GetMapping("/{id}/shares")
	@Operation(summary = "List the teams a note is shared with", description = "Owner only. Sorted by team name.")
	@ApiResponse(responseCode = "200", description = "The teams the note is shared with. Empty if it is private.")
	@ApiResponse(responseCode = "400", description = "The id is not a UUID.",
		content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ProblemDetail.class)))
	@ApiResponse(responseCode = "403", description = NOT_OWNER,
		content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ProblemDetail.class)))
	@ApiResponse(responseCode = "404", description = NOT_FOUND,
		content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ProblemDetail.class)))
	public List<ShareResponse> listShares(@Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser caller,
			@PathVariable UUID id) {
		return noteService.listShares(caller.id(), id);
	}

	@PutMapping("/{id}/shares/{teamId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@Operation(summary = "Share a note with a team",
		description = "Owner only. The team's members can then read the note. The owner does not have to be a member "
			+ "of the team. Sharing with a team the note is already shared with changes nothing.")
	@ApiResponse(responseCode = "204", description = "The note is shared with the team.")
	@ApiResponse(responseCode = "400", description = "An id is not a UUID.",
		content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ProblemDetail.class)))
	@ApiResponse(responseCode = "403", description = NOT_OWNER,
		content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ProblemDetail.class)))
	@ApiResponse(responseCode = "404", description = "There is no such note or team, or the caller is not allowed to see the note. The message says which.",
		content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ProblemDetail.class)))
	public void share(@Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser caller,
			@PathVariable UUID id, @PathVariable UUID teamId) {
		noteService.share(caller.id(), id, teamId);
	}

	@DeleteMapping("/{id}/shares/{teamId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@Operation(summary = "Stop sharing a note with a team",
		description = "Owner only. If the note was not shared with that team, nothing changes.")
	@ApiResponse(responseCode = "204", description = "The note is not shared with the team.")
	@ApiResponse(responseCode = "400", description = "An id is not a UUID.",
		content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ProblemDetail.class)))
	@ApiResponse(responseCode = "403", description = NOT_OWNER,
		content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ProblemDetail.class)))
	@ApiResponse(responseCode = "404", description = NOT_FOUND,
		content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ProblemDetail.class)))
	public void unshare(@Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser caller,
			@PathVariable UUID id, @PathVariable UUID teamId) {
		noteService.unshare(caller.id(), id, teamId);
	}

}
