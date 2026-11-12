# Implementation Plan

The order in which features are built, and a log of where the work stands. The design behind each step is in `DECISIONS.md`; the working rules are in `CLAUDE.md`.

## How this plan is used

- Steps are built in order, one at a time, test-first.
- A step is ticked off in the same commit that completes it, and the progress log at the bottom gets a line.
- **Checkpoints:** work stops for human review at the end of every phase. The checkpoint report covers what was built, the test results, anything that differs from the plan, and what comes next. The next phase does not start until it is approved.
- **Unplanned stops:** work also stops at once, in the middle of a phase, for any design question that `DECISIONS.md` does not already answer.
- Changing the plan (adding, dropping or reordering steps) needs approval first.

## Current position

**Phase 2, step 2.6** (not started).

## Phase 0: Foundation

- [x] 0.1 Scaffold the Spring Boot project (Gradle, Java 25, H2, Flyway, Security)
- [x] 0.2 Open health endpoint that checks the database (`GET /actuator/health`)
- [x] 0.3 Swagger UI at `/swagger-ui.html`, with HTTP Basic declared

## Phase 1: Users and authentication

- [x] 1.1 First Flyway migration: the `users` table
- [x] 1.2 `POST /api/v1/users`: register, hash the password, return the user without it
- [x] 1.3 Registration validation (`400`) and taken username (`409`)
- [x] 1.4 HTTP Basic checked against the `users` table
- [x] 1.5 `GET /api/v1/users/me`
- [x] **Checkpoint 1:** human review

## Phase 2: Notes

Notes come before teams because they are the core of the service and depend only on users.

- [x] 2.1 Migration: the `notes` table
- [x] 2.2 `POST /api/v1/notes`: create a private note, with validation; the response includes the owner (decision 039)
- [x] 2.3 `GET /api/v1/notes/{id}`: owner can read; anyone else gets `404`
- [x] 2.4 `PUT /api/v1/notes/{id}`: owner replaces title and body; stale `version` gives `409`
- [x] 2.5 `DELETE /api/v1/notes/{id}`: owner only
- [ ] 2.6 `GET /api/v1/notes`: list the caller's own notes, paged
- [ ] **Checkpoint 2:** human review

## Phase 3: Teams

- [ ] 3.1 Migration: the `teams` and `team_members` tables
- [ ] 3.2 `POST /api/v1/teams`: create a team, creator becomes the first member; name limits from decision 038; taken name gives `409`
- [ ] 3.3 `GET /api/v1/teams`: list all teams, paged
- [ ] 3.4 `GET /api/v1/users/me/teams`: list the teams the caller belongs to (decision 037)
- [ ] 3.5 `GET /api/v1/teams/{id}`: details and members, members only
- [ ] 3.6 `POST /api/v1/teams/{id}/members`: add a user by username, members only
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
- [ ] 5.5 Commit and push everything: the working tree is clean and `origin/main` matches the local branch
- [ ] 5.6 Clone the repository into an empty folder and confirm it is runnable from there: the build succeeds, the tests pass, the service starts and the health check answers
- [ ] 5.7 Confirm the reviewers can open the repository link
- [ ] **Checkpoint 5:** final human review before submission

## Progress log

| Date | Step | Note |
|---|---|---|
| 2026-11-11 | 0.1 | Project scaffolded; context-load test passes. |
| 2026-11-11 | 0.2 | Health endpoint added with Actuator; verified against the running service. |
| 2026-11-11 | 0.3 | Swagger UI added with springdoc-openapi 3.2.0; 7 tests passing. |
| 2026-11-11 | | Plan created. |
| 2026-11-11 | 1.1 | `users` table created by migration V1; 10 tests passing. |
| 2026-11-11 | 1.2 | `POST /api/v1/users` registers a user with a BCrypt-hashed password; 15 tests passing. |
| 2026-11-11 | 1.3 | Registration returns `400` for invalid input and `409` for a taken username, as problem-details JSON; 33 tests passing. |
| 2026-11-11 | 1.4 | HTTP Basic credentials are checked against the `users` table; 39 tests passing. |
| 2026-11-11 | 1.5 | `GET /api/v1/users/me` returns the calling user; 42 tests passing. Phase 1 flow verified against the running service. Stopped at checkpoint 1. |
| 2026-11-11 | | Whole-project review after phase 1. Plan changed with approval: notes (now phase 2) are built before teams (now phase 3); a "my teams" endpoint, team name limits and the note owner field were added; phase 5 gained push, fresh-clone and access checks. `gradlew` made executable for Mac and Linux. |
| 2026-11-11 | | Checkpoint 1 approved. Decisions 040 to 045 recorded. |
| 2026-11-11 | 2.1 | `notes` table created by migration V2; 47 tests passing. |
| 2026-11-11 | 2.2 | `POST /api/v1/notes` creates a private note and returns it with its owner; 57 tests passing. |
| 2026-11-11 | 2.3 | `GET /api/v1/notes/{id}` returns a note to its owner and `404` to anyone else; 64 tests passing. |
| 2026-11-11 | 2.4 | `PUT /api/v1/notes/{id}` replaces title and body and rejects a stale version with `409`; 76 tests passing. |
| 2026-11-11 | 2.5 | `DELETE /api/v1/notes/{id}` lets the owner delete a note; 82 tests passing. |
