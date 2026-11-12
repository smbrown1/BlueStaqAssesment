package com.bluestaq.notes.team;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** A team with its members, as shown to the team's members. */
public record TeamResponse(UUID id, String name, Instant createdAt, List<Member> members) {

	/** One member of the team. */
	public record Member(UUID id, String username, Instant addedAt) {
	}

	/** Must be called inside a transaction if the members' users are not already loaded. */
	static TeamResponse from(Team team, List<TeamMember> members) {
		List<Member> memberResponses = members.stream()
			.map(member -> new Member(member.getUser().getId(), member.getUser().getUsername(), member.getAddedAt()))
			.toList();
		return new TeamResponse(team.getId(), team.getName(), team.getCreatedAt(), memberResponses);
	}

}
