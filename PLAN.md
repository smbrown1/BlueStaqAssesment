# Implementation Plan

The order in which features are built, and a log of where the work stands. The design behind each step is in `DECISIONS.md`; the working rules are in `CLAUDE.md`.

## How this plan is used

- Steps are built in order, one at a time, test-first.
- A step is ticked off in the same commit that completes it, and the progress log at the bottom gets a line.
- **Checkpoints:** work stops for human review at the end of every phase. The checkpoint report covers what was built, the test results, anything that differs from the plan, and what comes next. The next phase does not start until it is approved.
- **Unplanned stops:** work also stops at once, in the middle of a phase, for any design question that `DECISIONS.md` does not already answer.
- Changing the plan (adding, dropping or reordering steps) needs approval first.

## Current position

**Phase 1, step 1.1** (not started). Phase 0 is complete.

## Phase 0: Foundation

- [x] 0.1 Scaffold the Spring Boot project (Gradle, Java 25, H2, Flyway, Security)
- [x] 0.2 Open health endpoint that checks the database (`GET /actuator/health`)
- [x] 0.3 Swagger UI at `/swagger-ui.html`, with HTTP Basic declared

## Phase 1: Users and authentication

- [ ] 1.1 First Flyway migration: the `users` table
- [ ] 1.2 `POST /api/v1/users`: register, hash the password, return the user without it
- [ ] 1.3 Registration validation (`400`) and taken username (`409`)
- [ ] 1.4 HTTP Basic checked against the `users` table
- [ ] 1.5 `GET /api/v1/users/me`
- [ ] **Checkpoint 1:** human review

## Phase 2: Teams

- [ ] 2.1 Migration: the `teams` and `team_members` tables
- [ ] 2.2 `POST /api/v1/teams`: create a team, creator becomes the first member; taken name gives `409`
- [ ] 2.3 `GET /api/v1/teams`: list all teams, paged
- [ ] 2.4 `GET /api/v1/teams/{id}`: details and members, members only
- [ ] 2.5 `POST /api/v1/teams/{id}/members`: add a user by username, members only
- [ ] **Checkpoint 2:** human review

## Phase 3: Notes

- [ ] 3.1 Migration: the `notes` table
- [ ] 3.2 `POST /api/v1/notes`: create a private note, with validation
- [ ] 3.3 `GET /api/v1/notes/{id}`: owner can read; anyone else gets `404`
- [ ] 3.4 `PUT /api/v1/notes/{id}`: owner replaces title and body; stale `version` gives `409`
- [ ] 3.5 `DELETE /api/v1/notes/{id}`: owner only
- [ ] 3.6 `GET /api/v1/notes`: list the caller's own notes, paged
- [ ] **Checkpoint 3:** human review

## Phase 4: Sharing

- [ ] 4.1 Migration: the `note_shares` table
- [ ] 4.2 `PUT /api/v1/notes/{id}/shares/{teamId}`: owner shares with a team; repeating is harmless
- [ ] 4.3 `DELETE /api/v1/notes/{id}/shares/{teamId}`: owner stops sharing
- [ ] 4.4 `GET /api/v1/notes/{id}/shares`: owner lists the teams
- [ ] 4.5 Team members can read a shared note; their attempts to change it give `403`
- [ ] 4.6 `GET /api/v1/notes` includes shared notes, with filters for owned, shared, or one team
- [ ] 4.7 Deleting a note removes its shares
- [ ] **Checkpoint 4:** human review

## Phase 5: Finish

- [ ] 5.1 Run through the whole flow against the running service from an empty database
- [ ] 5.2 Check every endpoint in Swagger UI against the list in decision 023
- [ ] 5.3 Complete the README: endpoint summary, a walkthrough, final design choices and more-time list
- [ ] 5.4 Fill in the rationale entries still marked "None recorded yet" in `DECISIONS.md`
- [ ] **Checkpoint 5:** final human review before submission

## Progress log

| Date | Step | Note |
|---|---|---|
| 2026-11-11 | 0.1 | Project scaffolded; context-load test passes. |
| 2026-11-11 | 0.2 | Health endpoint added with Actuator; verified against the running service. |
| 2026-11-11 | 0.3 | Swagger UI added with springdoc-openapi 3.2.0; 7 tests passing. |
| 2026-11-11 | | Plan created. |
