package com.bluestaq.notes.team;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface TeamMemberRepository extends JpaRepository<TeamMember, TeamMemberId> {

	/** A team's members with their users loaded, in the order they joined. */
	@Query("""
		select m from TeamMember m join fetch m.user u
		where m.id.teamId = :teamId
		order by m.addedAt, u.username
		""")
	List<TeamMember> findByTeamId(UUID teamId);

}
