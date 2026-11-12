package com.bluestaq.notes.team;

import java.util.Locale;
import java.util.UUID;

import com.bluestaq.notes.error.ConflictException;
import com.bluestaq.notes.error.ForbiddenException;
import com.bluestaq.notes.error.NotFoundException;
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

	/** Lists the teams the caller belongs to, by name (decision 037). */
	@Transactional(readOnly = true)
	public Page<TeamSummary> listMine(UUID callerId, int page, int size) {
		return teams.findByMember(callerId, Paging.request(page, size, BY_NAME)).map(TeamSummary::from);
	}

	/**
	 * Returns a team with its members. Only members may see it.
	 *
	 * @throws NotFoundException if there is no such team
	 * @throws ForbiddenException if the caller is not a member
	 */
	@Transactional(readOnly = true)
	public TeamResponse get(UUID callerId, UUID teamId) {
		Team team = findAsMember(callerId, teamId);
		return TeamResponse.from(team, members.findByTeamId(teamId));
	}

	/**
	 * Adds a user to a team. Any member may add anyone (decision 020). Adding someone who is
	 * already a member changes nothing and still succeeds (decision 052).
	 *
	 * @throws NotFoundException if there is no such team, or no user with that username
	 * @throws ForbiddenException if the caller is not a member
	 */
	@Transactional
	public TeamResponse addMember(UUID callerId, UUID teamId, String username) {
		Team team = findAsMember(callerId, teamId);
		String normalized = username.strip().toLowerCase(Locale.ROOT);
		User user = users.findByUsername(normalized)
			.orElseThrow(() -> new NotFoundException("No user with the username '" + normalized + "'."));
		if (!isMember(teamId, user.getId())) {
			members.saveAndFlush(new TeamMember(teamId, user));
		}
		return TeamResponse.from(team, members.findByTeamId(teamId));
	}

	/** Whether the user belongs to the team. */
	@Transactional(readOnly = true)
	public boolean isMember(UUID teamId, UUID userId) {
		return members.existsById(new TeamMemberId(teamId, userId));
	}

	/**
	 * Finds a team on behalf of a caller who must be one of its members. Team ids and names are
	 * visible to everyone, so a non-member is told they are not allowed rather than that the
	 * team does not exist (decision 053).
	 */
	private Team findAsMember(UUID callerId, UUID teamId) {
		Team team = teams.findById(teamId).orElseThrow(() -> new NotFoundException("Team not found."));
		if (!isMember(teamId, callerId)) {
			throw new ForbiddenException("You are not a member of this team.");
		}
		return team;
	}

	private static ConflictException nameTaken(String name) {
		return new ConflictException("A team named '" + name + "' already exists.");
	}

}
