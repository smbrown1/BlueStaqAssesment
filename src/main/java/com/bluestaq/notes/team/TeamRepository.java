package com.bluestaq.notes.team;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface TeamRepository extends JpaRepository<Team, UUID> {

	boolean existsByNameKey(String nameKey);

	/** The teams a user is a member of. */
	@Query("""
		select t from Team t
		where exists (select 1 from TeamMember m where m.id.teamId = t.id and m.id.userId = :userId)
		""")
	Page<Team> findByMember(UUID userId, Pageable pageable);

}
