package com.bluestaq.notes.team;

import java.util.UUID;

import com.bluestaq.notes.error.ConflictException;
import com.bluestaq.notes.paging.Paging;
import com.bluestaq.notes.user.User;
import com.bluestaq.notes.user.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Team operations. Each method takes the id of the calling user and enforces what that user may do.
 */
@Service
public class TeamService {

	/** The fixed order of every team list: by name, ignoring case. The key is unique, so pages never overlap. */
	private static final Sort BY_NAME = Sort.by("nameKey");

	private final TeamRepository teams;
	private final TeamMemberRepository members;
	private final UserRepository users;

	public TeamService(TeamRepository teams, TeamMemberRepository members, UserRepository users) {
		this.teams = teams;
		this.members = members;
		this.users = users;
	}

	/**
	 * Creates a team and makes the caller its first member (decision 020).
	 *
	 * @throws ConflictException if a team with the same name, ignoring case, already exists
	 */
	@Transactional
	public TeamResponse create(UUID callerId, String name) {
		if (teams.existsByNameKey(Team.keyFor(name))) {
			throw nameTaken(name);
		}
		User caller = users.getReferenceById(callerId);
		Team team;
		try {
			team = teams.saveAndFlush(new Team(name, caller));
		}
		catch (DataIntegrityViolationException ex) {
			// Two requests for the same name arrived together and the other one won.
			throw nameTaken(name);
		}
		members.saveAndFlush(new TeamMember(team.getId(), caller));
		return TeamResponse.from(team, members.findByTeamId(team.getId()));
	}

	/** Lists every team, by name. Any user may see every team's id and name (decision 025). */
	@Transactional(readOnly = true)
	public Page<TeamSummary> listAll(int page, int size) {
		return teams.findAll(Paging.request(page, size, BY_NAME)).map(TeamSummary::from);
	}

	private static ConflictException nameTaken(String name) {
		return new ConflictException("A team named '" + name + "' already exists.");
	}

}
