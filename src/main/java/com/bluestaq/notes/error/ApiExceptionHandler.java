package com.bluestaq.notes.error;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Turns exceptions into problem-details JSON responses (RFC 9457).
 * The base class already does this for Spring's own errors, such as an unreadable body.
 */
@RestControllerAdvice
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {

	/** Adds an "errors" object naming each invalid field, so the caller can see what to fix. */
	@Override
	protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
			HttpHeaders headers, HttpStatusCode status, WebRequest request) {
		Map<String, String> errors = new LinkedHashMap<>();
		for (FieldError error : ex.getBindingResult().getFieldErrors()) {
			errors.merge(error.getField(), error.getDefaultMessage(), (first, next) -> first + "; " + next);
		}
		ProblemDetail problem = ex.getBody();
		problem.setProperty("errors", errors);
		return handleExceptionInternal(ex, problem, headers, status, request);
	}

	@ExceptionHandler(NotFoundException.class)
	ProblemDetail handleNotFound(NotFoundException ex) {
		return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
	}

	@ExceptionHandler(ConflictException.class)
	ProblemDetail handleConflict(ConflictException ex) {
		return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
	}

}
