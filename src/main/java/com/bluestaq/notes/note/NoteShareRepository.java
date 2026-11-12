package com.bluestaq.notes.note;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface NoteShareRepository extends JpaRepository<NoteShare, NoteShareId> {

	/** The teams a note is shared with, with each team loaded, by team name. */
	@Query("""
		select s from NoteShare s join fetch s.team t
		where s.id.noteId = :noteId
		order by t.nameKey
		""")
	List<NoteShare> findByNoteId(UUID noteId);

	/** Whether the note is shared with at least one team the user belongs to. */
	@Query("""
		select count(s) > 0 from NoteShare s
		where s.id.noteId = :noteId
		  and exists (select 1 from TeamMember m where m.id.teamId = s.id.teamId and m.id.userId = :userId)
		""")
	boolean isSharedWithATeamOf(UUID noteId, UUID userId);

}
