package com.bluestaq.notes.note;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

/**
 * Every list loads each note's owner in the same query, because the owner is part of the response.
 */
public interface NoteRepository extends JpaRepository<Note, UUID> {

	/** True for a note that is shared with at least one team the user belongs to. */
	String SHARED_WITH_A_TEAM_OF_THE_USER = """
		exists (select 1 from NoteShare s, TeamMember m
		        where s.id.noteId = n.id and m.id.teamId = s.id.teamId and m.id.userId = :userId)
		""";

	/** The notes a user owns. */
	@EntityGraph(attributePaths = "owner")
	Page<Note> findByOwnerId(UUID ownerId, Pageable pageable);

	/** Every note a user may read: their own, and those shared with their teams. */
	@EntityGraph(attributePaths = "owner")
	@Query("select n from Note n where n.owner.id = :userId or " + SHARED_WITH_A_TEAM_OF_THE_USER)
	Page<Note> findReadableBy(UUID userId, Pageable pageable);

	/** Other people's notes that are shared with a user's teams. */
	@EntityGraph(attributePaths = "owner")
	@Query("select n from Note n where n.owner.id <> :userId and " + SHARED_WITH_A_TEAM_OF_THE_USER)
	Page<Note> findSharedWith(UUID userId, Pageable pageable);

	/** The notes shared with one team. */
	@EntityGraph(attributePaths = "owner")
	@Query("""
		select n from Note n
		where exists (select 1 from NoteShare s where s.id.noteId = n.id and s.id.teamId = :teamId)
		""")
	Page<Note> findSharedWithTeam(UUID teamId, Pageable pageable);

}
