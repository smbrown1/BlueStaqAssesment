# BlueStaq Assessment

Backend for a note-taking service shared among several small teams. Built with Java 25, Spring Boot and Gradle. The task is described in `Requirements.md`, and every design decision is logged in `DECISIONS.md`.

## Running the service

You need a JDK (17 or newer) installed, with `JAVA_HOME` pointing at it, to launch Gradle. The build itself uses Java 25 and downloads it if it is missing.

```
./gradlew bootRun
```

On Windows use `gradlew.bat bootRun`. The service listens on port 8080 and stores its data in `./data`.

Check that it is up and connected to its database (no credentials needed):

```
curl http://localhost:8080/actuator/health
```

A healthy service answers `200` with `"status":"UP"`, including `"db":{"status":"UP"}`.

Browse and try the endpoints in Swagger UI at http://localhost:8080/swagger-ui.html. Use its Authorize button to enter a username and password.

Run the tests with `./gradlew test`.

## Endpoint guide

All `/api/v1` endpoints require HTTP Basic authentication except user registration. Use Swagger UI to inspect request and response fields and try requests interactively.

### Available now

| Method | Path | Purpose | Access |
|---|---|---|---|
| `GET` | `/actuator/health` | Check service and database health. | Public |
| `POST` | `/api/v1/users` | Register with a username and password. | Public |
| `GET` | `/api/v1/users/me` | Get the authenticated user's profile. | Authenticated |
| `POST` | `/api/v1/notes` | Create a private note. | Authenticated; owner is the caller |
| `GET` | `/api/v1/notes` | List notes the caller can read, paged. Supports `scope=all`, `owned`, or `shared`, and optional `teamId`. | Authenticated; `teamId` requires team membership |
| `GET` | `/api/v1/notes/{id}` | Read a note. | Owner or member of a team it is shared with |
| `PUT` | `/api/v1/notes/{id}` | Replace a note's title and body using its current version. | Owner only |
| `DELETE` | `/api/v1/notes/{id}` | Delete a note. | Owner only |
| `POST` | `/api/v1/teams` | Create a team; the creator becomes its first member. | Authenticated |
| `GET` | `/api/v1/teams` | List all teams, paged. | Authenticated |
| `GET` | `/api/v1/users/me/teams` | List the teams the caller belongs to, paged. | Authenticated |
| `GET` | `/api/v1/teams/{id}` | Get a team's details and members. | Team members only |
| `POST` | `/api/v1/teams/{id}/members` | Add a user to a team. | Team members only |
| `PUT` | `/api/v1/notes/{id}/shares/{teamId}` | Share a note with a team. | Owner only |
| `DELETE` | `/api/v1/notes/{id}/shares/{teamId}` | Stop sharing a note with a team. | Owner only |
| `GET` | `/api/v1/notes/{id}/shares` | List the teams a note is shared with. | Owner only |

## Project progress

The implementation for phases 0–4 is present, including users, notes, teams and note sharing. The test suite passes: 188 tests across 27 suites. Human review checkpoints for phases 3 and 4 are still pending. Phase 5 remains: verify the complete flow and Swagger docs, add a walkthrough, and confirm a fresh clone and reviewer access.

### Remaining work

- **Phase 3 — Teams:** implementation is complete; human review checkpoint pending.
- **Phase 4 — Sharing:** implementation is complete; human review checkpoint pending.
- **Phase 5 — Finish:** verify the full flow and Swagger documentation, add a walkthrough, review the final design choices and more-time list, fill in missing decision rationales, commit and push, verify a fresh clone and reviewer access, then stop for final review.
## Design choices I spent the most time on

### 1. Storage: H2, an embedded SQL database

The service stores its data in H2, a SQL database that runs inside the application's own process, accessed through Spring Data JPA.

Why H2:

- **The service runs with no setup.** H2 ships as a library, so a reviewer does not need to install a database, start Docker, or configure a connection. The brief asks for a runnable service, and every extra setup step is a chance for it to fail on someone else's machine.
- **Tests are fast and isolated.** Each test run gets a fresh in-memory database, which suits the test-driven approach used throughout.
- **The data is still relational.** Notes, users and teams and the relationships between them fit SQL tables and foreign keys, so the schema can be judged on its own merits.

What I gave up:

- **H2 is not a production database.** It handles concurrent writes poorly and has no replication or backup tooling.
- **Its SQL is not identical to PostgreSQL's.** Code that passes on H2 can behave differently on a real database, so a move to PostgreSQL is not proven until the tests run against it.

The alternative I weighed was PostgreSQL through Docker Compose. It is closer to production, but reviewers would need Docker running, and the tests would need either Docker or a second database anyway. I chose a service that is easy to run over one that is closer to production. Because all data access goes through JPA, the Java code would not change in a swap: it is a driver dependency, connection settings, and a test run against the new database.

### 2. Sharing model: a user owns a note and shares it with teams

The brief says the service is "used and shared amongst several small teams", and leaves open what sharing means. The use case I designed for is that one user writes their notes and then shares them with a team.

The model:

- **A note belongs to the user who wrote it.** It is private until the owner shares it.
- **The owner can share a note with any number of teams**, including teams they are not a member of, so a note can be passed from one team to another.
- **Members of those teams can read the note. Only the owner can edit, delete, share or unshare it.**
- **Anyone can register.** The user who creates a team is its first member, and any member can add others.

Why this model:

- **It matches how notes get written.** Someone drafts a note on their own and shares it when it is ready. The simpler alternative, where every note belongs to exactly one team, has no private notes and cannot share one note with two teams.
- **Permission checks stay small.** A caller is either the owner or a reader, so there are two cases to build and test. Per-user permissions on each note would be more flexible, but that is more than several small teams need.
- **Read-only sharing is the smallest version that delivers the use case.** It keeps the first version focused on getting ownership and visibility right.

What I gave up:

- **Teammates cannot edit a shared note.** Collaboration on one note has to go through its owner.
- **Team members have no roles.** Any member can add members, and nobody is in charge of a team.
- **A team cannot refuse a note.** Because owners can share with teams they do not belong to, a team can receive notes it did not ask for.

## What I'd change or add with more time

- **Add PostgreSQL for real use and stop using H2 outside this assessment.** H2 is a deliberate convenience for a reviewer running the assessment locally, not a long-term storage choice. In a real deployment I would replace it with PostgreSQL, provide it through Docker Compose, and run the tests against PostgreSQL so database-specific behavior is exercised.
- **Add team roles.** Team members currently have no roles. With more time I would add team admins and members, so that only admins can change membership or remove a team.
- **Let team members edit shared notes.** A shared note is currently read-only for everyone but its owner. With more time I would let the owner grant edit access to a team.
- **Let members be removed from a team, or leave it.** Team membership can currently only grow.
- **Let users change their password or delete their account.**

## What I'd stop doing

- **Stop using H2 for a deployed service.** Its embedded setup is useful for this assessment, but PostgreSQL is the database I would use for ongoing real-world operation, backups, and concurrent writes.

Nice to have:

- **Text search** across note titles and bodies.
- **Tags** for organising notes.
- **Edit history**, so earlier versions of a note can be viewed and restored.
- **Soft delete**, so a deleted note can be recovered.
