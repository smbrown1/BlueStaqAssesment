package com.bluestaq.notes.note;

import java.util.UUID;

import com.bluestaq.notes.security.AuthenticatedUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
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
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/notes")
@Tag(name = "Notes")
@ApiResponse(responseCode = "401", description = "The credentials are missing or wrong.", content = @Content)
public class NoteController {

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

	@GetMapping("/{id}")
	@Operation(summary = "Read a note")
	@ApiResponse(responseCode = "200", description = "The note.")
	@ApiResponse(responseCode = "400", description = "The id is not a UUID.",
		content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ProblemDetail.class)))
	@ApiResponse(responseCode = "404", description = "There is no such note, or the caller is not allowed to see it.",
		content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ProblemDetail.class)))
	public NoteResponse get(@Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser caller,
			@PathVariable UUID id) {
		return noteService.get(caller.id(), id);
	}

	@PutMapping("/{id}")
	@Operation(summary = "Replace a note's title and body",
		description = "Send the version the note had when you read it. If someone has saved the note since, "
			+ "the update is rejected with 409 rather than overwriting their change.")
	@ApiResponse(responseCode = "200", description = "The updated note, with its new version.")
	@ApiResponse(responseCode = "400", description = "The title, body or version breaks the rules, or the id is not a UUID.",
		content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ProblemDetail.class)))
	@ApiResponse(responseCode = "404", description = "There is no such note, or the caller is not allowed to see it.",
		content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ProblemDetail.class)))
	@ApiResponse(responseCode = "409", description = "The note has been saved since the version that was sent.",
		content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ProblemDetail.class)))
	public NoteResponse update(@Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser caller,
			@PathVariable UUID id, @Valid @RequestBody UpdateNoteRequest request) {
		return noteService.update(caller.id(), id, request.title(), request.body(), request.version());
	}

	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@Operation(summary = "Delete a note")
	@ApiResponse(responseCode = "204", description = "The note was deleted.")
	@ApiResponse(responseCode = "400", description = "The id is not a UUID.",
		content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ProblemDetail.class)))
	@ApiResponse(responseCode = "404", description = "There is no such note, or the caller is not allowed to see it.",
		content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ProblemDetail.class)))
	public void delete(@Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser caller,
			@PathVariable UUID id) {
		noteService.delete(caller.id(), id);
	}

}
