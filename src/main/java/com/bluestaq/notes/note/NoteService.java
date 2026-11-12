package com.bluestaq.notes.note;

import java.util.UUID;

import com.bluestaq.notes.error.NotFoundException;
import com.bluestaq.notes.user.User;
import com.bluestaq.notes.user.UserRepository;
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
	 * Finds a note the caller may read. A note the caller may not see is reported exactly
	 * like one that does not exist, so its existence is not revealed (decision 026).
	 */
	private Note findReadable(UUID callerId, UUID noteId) {
		return notes.findById(noteId)
			.filter(note -> note.getOwner().getId().equals(callerId))
			.orElseThrow(() -> new NotFoundException("Note not found."));
	}

}
