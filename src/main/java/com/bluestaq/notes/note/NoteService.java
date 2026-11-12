package com.bluestaq.notes.note;

import java.util.List;
import java.util.UUID;

import com.bluestaq.notes.error.ConflictException;
import com.bluestaq.notes.error.ForbiddenException;
import com.bluestaq.notes.error.NotFoundException;
import com.bluestaq.notes.paging.Paging;
import com.bluestaq.notes.team.Team;
import com.bluestaq.notes.team.TeamRepository;
import com.bluestaq.notes.team.TeamService;
import com.bluestaq.notes.user.User;
import com.bluestaq.notes.user.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Note operations. Each method takes the id of the calling user and enforces what that user may do:
 * the owner can do anything with a note, and members of a team it is shared with can read it (decision 017).
 * Methods return API responses, built inside the transaction while the note's owner can still be read.
 */
@Service
public class NoteService {

	/** The fixed order of every note list (decision 044). The id breaks ties so pages never overlap. */
	private static final Sort MOST_RECENTLY_UPDATED_FIRST =
		Sort.by(Sort.Order.desc("updatedAt"), Sort.Order.asc("id"));

	private final NoteRepository notes;
	private final NoteShareRepository shares;
	private final UserRepository users;
	private final TeamRepository teams;
	private final TeamService teamService;

	public NoteService(NoteRepository notes, NoteShareRepository shares, UserRepository users,
			TeamRepository teams, TeamService teamService) {
		this.notes = notes;
		this.shares = shares;
		this.users = users;
		this.teams = teams;
		this.teamService = teamService;
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
	 * Lists notes the caller may read, most recently updated first.
	 * Page and size values out of range are brought into range (see {@link Paging}).
	 *
	 * @param scope which of the caller's readable notes to include; ignored when a team is given
	 * @param teamId if not null, list only the notes shared with this team, which the caller must belong to
	 * @throws NotFoundException if the team does not exist
	 * @throws ForbiddenException if the caller is not a member of the team
	 */
	@Transactional(readOnly = true)
	public Page<NoteResponse> list(UUID callerId, NoteScope scope, UUID teamId, int page, int size) {
		PageRequest pageRequest = Paging.request(page, size, MOST_RECENTLY_UPDATED_FIRST);
		Page<Note> found;
		if (teamId != null) {
			findTeam(teamId);
			if (!teamService.isMember(teamId, callerId)) {
				throw new ForbiddenException("You are not a member of this team.");
			}
			found = notes.findSharedWithTeam(teamId, pageRequest);
		}
		else {
			found = switch (scope) {
				case ALL -> notes.findReadableBy(callerId, pageRequest);
				case OWNED -> notes.findByOwnerId(callerId, pageRequest);
				case SHARED -> notes.findSharedWith(callerId, pageRequest);
			};
		}
		return found.map(NoteResponse::from);
	}

	/**
	 * Replaces a note's title and body. A missing body is stored as empty.
	 *
	 * @param expectedVersion the version the caller last read
	 * @throws NotFoundException if there is no such note, or the caller may not see it
	 * @throws ForbiddenException if the caller can read the note but does not own it
	 * @throws ConflictException if the note has been saved since the caller read it
	 */
	@Transactional
	public NoteResponse update(UUID callerId, UUID noteId, String title, String body, long expectedVersion) {
		Note note = findOwned(callerId, noteId);
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
	 * Deletes a note. Its shares go with it, removed by the database.
	 *
	 * @throws NotFoundException if there is no such note, or the caller may not see it
	 * @throws ForbiddenException if the caller can read the note but does not own it
	 */
	@Transactional
	public void delete(UUID callerId, UUID noteId) {
		notes.delete(findOwned(callerId, noteId));
	}

	/**
	 * Lists the teams a note is shared with. Only the owner may ask.
	 *
	 * @throws NotFoundException if there is no such note, or the caller may not see it
	 * @throws ForbiddenException if the caller can read the note but does not own it
	 */
	@Transactional(readOnly = true)
	public List<ShareResponse> listShares(UUID callerId, UUID noteId) {
		findOwned(callerId, noteId);
		return shares.findByNoteId(noteId).stream().map(ShareResponse::from).toList();
	}

	/**
	 * Shares a note with a team, which the owner need not belong to (decision 019).
	 * Sharing with a team it is already shared with changes nothing.
	 *
	 * @throws NotFoundException if there is no such note or team, or the caller may not see the note
	 * @throws ForbiddenException if the caller can read the note but does not own it
	 */
	@Transactional
	public void share(UUID callerId, UUID noteId, UUID teamId) {
		findOwned(callerId, noteId);
		Team team = findTeam(teamId);
		if (!shares.existsById(new NoteShareId(noteId, teamId))) {
			shares.saveAndFlush(new NoteShare(noteId, team));
		}
	}

	/**
	 * Stops sharing a note with a team. If it was not shared with that team, nothing changes.
	 *
	 * @throws NotFoundException if there is no such note, or the caller may not see it
	 * @throws ForbiddenException if the caller can read the note but does not own it
	 */
	@Transactional
	public void unshare(UUID callerId, UUID noteId, UUID teamId) {
		findOwned(callerId, noteId);
		shares.findById(new NoteShareId(noteId, teamId)).ifPresent(shares::delete);
	}

	/**
	 * Finds a note the caller may read: their own, or one shared with a team they belong to.
	 * A note the caller may not see is reported exactly like one that does not exist,
	 * so its existence is not revealed (decision 026).
	 */
	private Note findReadable(UUID callerId, UUID noteId) {
		return notes.findById(noteId)
			.filter(note -> isOwner(note, callerId) || shares.isSharedWithATeamOf(noteId, callerId))
			.orElseThrow(() -> new NotFoundException("Note not found."));
	}

	/**
	 * Finds a note the caller owns. A caller who can read the note but does not own it already
	 * knows it exists, so they are told they are not allowed (decision 026).
	 */
	private Note findOwned(UUID callerId, UUID noteId) {
		Note note = findReadable(callerId, noteId);
		if (!isOwner(note, callerId)) {
			throw new ForbiddenException("Only the note's owner can change or share it.");
		}
		return note;
	}

	private static boolean isOwner(Note note, UUID userId) {
		return note.getOwner().getId().equals(userId);
	}

	private Team findTeam(UUID teamId) {
		return teams.findById(teamId).orElseThrow(() -> new NotFoundException("Team not found."));
	}

	private static ConflictException staleVersion() {
		return new ConflictException(
			"The note has been changed since you read it. Read it again and reapply your changes.");
	}

}
