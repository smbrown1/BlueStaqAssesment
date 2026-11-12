package com.bluestaq.notes.note;

import java.time.Instant;

import com.bluestaq.notes.team.TeamSummary;

/** One team a note is shared with, and when it was shared. */
public record ShareResponse(TeamSummary team, Instant sharedAt) {

	/** Must be called inside a transaction if the share's team is not already loaded. */
	static ShareResponse from(NoteShare share) {
		return new ShareResponse(new TeamSummary(share.getTeam().getId(), share.getTeam().getName()), share.getSharedAt());
	}

}
