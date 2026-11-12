package com.bluestaq.notes.note;

import java.util.UUID;

import com.bluestaq.notes.error.ConflictException;
import com.bluestaq.notes.error.NotFoundException;
import com.bluestaq.notes.user.User;
import com.bluestaq.notes.user.UserRepository;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Note operations. Each method takes the id of the calling user and enforces what that user may do.
 * Methods return API responses, built inside the transaction while the note's owner can still be read.
 */
@Service
public class NoteService {

	private final NoteRepository notes;
	private final UserRepository users;

	public NoteService(NoteRepository notes, UserRepository users) {
		this.notes = notes;
		this.users = users;
	}

	/** Creates a private note owned by the caller. A missing body is stored as empty. */
	@Transactional
	public NoteResponse create(UUID callerId, String title, String body) {
		User owner = users.getReferenceById(callerId);
		Note note = notes.saveAndFlush(new Note(owner, title, body == null ? "" : body));
		return NoteResponse.from(note);
	}

	/**
	 * Returns a note the caller is allowed to read.
	 *
	 * @throws NotFoundException if there is no such note, or the caller may not see it
	 */
	@Transactional(readOnly = true)
	public NoteResponse get(UUID callerId, UUID noteId) {
		return NoteResponse.from(findReadable(callerId, noteId));
	}

	/**
	 * Replaces a note's title and body. A missing body is stored as empty.
	 *
	 * @param expectedVersion the version the caller last read
	 * @throws NotFoundException if there is no such note, or the caller may not see it
	 * @throws ConflictException if the note has been saved since the caller read it
	 */
	@Transactional
	public NoteResponse update(UUID callerId, UUID noteId, String title, String body, long expectedVersion) {
		Note note = findReadable(callerId, noteId);
		if (note.getVersion() != expectedVersion) {
			throw staleVersion();
		}
		note.replaceContent(title, body == null ? "" : body);
		try {
			// Flushing now makes the database check the version: the UPDATE only matches the row
			// if its version is still the one read above.
			notes.saveAndFlush(note);
		}
		catch (ObjectOptimisticLockingFailureException ex) {
			// Another save committed between the check above and this one.
			throw staleVersion();
		}
		return NoteResponse.from(note);
	}

	/**
	 * Deletes a note.
	 *
	 * @throws NotFoundException if there is no such note, or the caller may not see it
	 */
	@Transactional
	public void delete(UUID callerId, UUID noteId) {
		notes.delete(findReadable(callerId, noteId));
	}

	private static ConflictException staleVersion() {
		return new ConflictException(
			"The note has been changed since you read it. Read it again and reapply your changes.");
	}

	/**
	 * Finds a note the caller may read. A note the caller may not see is reported exactly
	 * like one that does not exist, so its existence is not revealed (decision 026).
	 */
	private Note findReadable(UUID callerId, UUID noteId) {
		return notes.findById(noteId)
			.filter(note -> note.getOwner().getId().equals(callerId))
			.orElseThrow(() -> new NotFoundException("Note not found."));
	}

}
