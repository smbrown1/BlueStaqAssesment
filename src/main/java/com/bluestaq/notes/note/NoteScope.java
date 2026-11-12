package com.bluestaq.notes.note;

import java.util.Locale;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/** Which notes a list should contain. */
public enum NoteScope {

	/** The caller's own notes and those shared with the caller's teams. */
	ALL,

	/** Only the notes the caller owns. */
	OWNED,

	/** Only other people's notes that are shared with the caller's teams. */
	SHARED;

	/** Reads the value of the {@code scope} query parameter, ignoring case. An unknown value is a 400. */
	public static NoteScope parse(String value) {
		try {
			return valueOf(value.strip().toUpperCase(Locale.ROOT));
		}
		catch (IllegalArgumentException ex) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "scope must be one of: all, owned, shared.");
		}
	}

}
