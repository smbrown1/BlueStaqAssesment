package com.bluestaq.notes.team;

import java.util.UUID;

/** A team as it appears in a list: its id and name only, with no members (decision 025). */
public record TeamSummary(UUID id, String name) {

	static TeamSummary from(Team team) {
		return new TeamSummary(team.getId(), team.getName());
	}

}
