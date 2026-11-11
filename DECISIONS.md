# Decision Log

A record of the design decisions made for this project, so each one can be explained and defended. Newest entries go at the bottom.

Each entry records:

- **Decision:** what was chosen.
- **Options considered:** the alternatives that were on the table.
- **Rationale:** why this option won, including the tradeoffs accepted.
- **Decided by / date:** who approved it and when.

---

## 001: Working rules for AI assistance

- **Decision:** Claude follows the rules in `CLAUDE.md`: frequent commits with explanations, red to green test-driven development, and no design decisions without human approval.
- **Options considered:** None recorded.
- **Rationale:** None recorded yet.
- **Decided by / date:** Steven Brown, 2026-11-11

## 002: Language is Java

- **Decision:** The service is written in Java.
- **Options considered:** None recorded.
- **Rationale:** None recorded yet.
- **Decided by / date:** Steven Brown, 2026-11-11

## 003: Framework is Spring Boot with Spring MVC

- **Decision:** Use Spring Boot, on the blocking Spring MVC stack rather than reactive WebFlux.
- **Options considered:** Spring Boot (MVC or WebFlux), Quarkus, Micronaut, Javalin.
- **Rationale:** Spring Boot is the industry default, so reviewers can read it without ramp-up. Validation, persistence, security and test support (MockMvc, `@SpringBootTest`) come built in, which keeps effort on the notes domain and suits test-driven development. Accepted tradeoffs: slower startup and higher memory use than the lighter frameworks, and behaviour that comes from auto-configuration has to be understood well enough to explain. Quarkus and Micronaut offer startup and memory gains that do not matter for a small team service. Javalin is more explicit but needs validation, persistence and dependency injection assembled by hand. MVC was chosen over WebFlux because reactive code only pays off at high concurrency and is harder to read and test.
- **Decided by / date:** Steven Brown, 2026-11-11

## 004: Build tool is Gradle

- **Decision:** Build with Gradle, using the Gradle wrapper so no local install is needed.
- **Options considered:** Gradle, Maven.
- **Rationale:** None recorded yet.
- **Decided by / date:** Steven Brown, 2026-11-11

## 005: Use the most recent compatible versions

- **Decision:** Use the newest stable releases that work together. As checked on 2026-11-11: Spring Boot 4.1.2 (4.2.0 is still a release candidate) and Gradle 9.9.1. The Java version is covered by decision 006.
- **Options considered:** Latest stable releases, or older maintained lines (Spring Boot 4.0.x).
- **Rationale:** None recorded yet.
- **Decided by / date:** Steven Brown, 2026-11-11

## 006: Java version is 25

- **Decision:** Target Java 25, with a Gradle toolchain so the build fetches the right JDK if it is not installed.
- **Options considered:** Java 25, Java 27, Java 26. Spring Boot 4.1.2 supports Java 17, 21, 25 and 27.
- **Rationale:** Java 25 is the newest long-term-support release, so reviewers are most likely to have it or be willing to install it. Java 27 is newer but less likely to be on a reviewer's machine. Java 26 is the only JDK installed on the development machine, but it is a short-lived release that Spring Boot does not list as supported. This narrows decision 005: for Java, "most recent" means most recent long-term-support release.
- **Decided by / date:** Steven Brown, 2026-11-11

## 007: Storage is H2

- **Decision:** Store data in H2, an embedded SQL database, accessed through Spring Data JPA. Moving to PostgreSQL is listed in the README as the first thing to change with more time.
- **Options considered:** H2, PostgreSQL through Docker Compose, SQLite, H2 by default with an optional PostgreSQL profile.
- **Rationale:** H2 runs inside the application, so reviewers can start the service with no database install, no Docker and no connection settings. Tests get a fresh in-memory database on each run. The data model is relational either way. Accepted tradeoffs: H2 is not fit for production (weak under concurrent writes, no replication or backup tooling), and its SQL differs from PostgreSQL's, so a later swap is only proven once the tests run against PostgreSQL. PostgreSQL was rejected for now because it needs Docker for both running and testing. The swap is kept cheap by doing all data access through JPA.
- **Decided by / date:** Steven Brown, 2026-11-11

## 008: H2 runs file-based, with in-memory for tests

- **Decision:** The running service uses a file-based H2 database. Tests use an in-memory H2 database.
- **Options considered:** File-based, in-memory.
- **Rationale:** File-based storage means notes survive a restart, which a note-taking service needs. In-memory storage gives each test run a fresh, isolated database.
- **Decided by / date:** Steven Brown, 2026-11-11

## 009: H2 runs in PostgreSQL compatibility mode

- **Decision:** Turn on H2's PostgreSQL compatibility mode.
- **Options considered:** Compatibility mode on, H2's native mode.
- **Rationale:** PostgreSQL is the intended production database (decision 007). Compatibility mode catches some differences between the two early, which keeps the later swap cheap. It is only partial, so it does not replace running the tests against PostgreSQL.
- **Decided by / date:** Steven Brown, 2026-11-11

## 010: Schema is managed by Flyway migrations

- **Decision:** Create and change the schema through Flyway migration scripts written in standard SQL. Hibernate does not generate tables.
- **Options considered:** Flyway migrations, Hibernate automatic schema generation.
- **Rationale:** Migrations give a versioned schema that reviewers can read directly. Hibernate picks different column types on different databases, so explicit standard SQL keeps the schema the same on H2 and PostgreSQL. Accepted tradeoff: one more dependency, and each schema change needs a migration file.
- **Decided by / date:** Steven Brown, 2026-11-11

## 011: Gradle build scripts use the Kotlin DSL

- **Decision:** Write the build scripts in Kotlin (`build.gradle.kts`), not Groovy.
- **Options considered:** Kotlin DSL, Groovy DSL.
- **Rationale:** Kotlin is Gradle's default for new projects and gives type checking and editor completion in the build file.
- **Decided by / date:** Steven Brown, 2026-11-11

## 012: Base package is com.bluestaq.notes

- **Decision:** All application code lives under the base package `com.bluestaq.notes`. The Gradle group is `com.bluestaq` and the artifact name is `notes`.
- **Options considered:** None recorded.
- **Rationale:** None recorded yet.
- **Decided by / date:** Steven Brown, 2026-11-11

## 013: Authentication is HTTP Basic with Spring Security

- **Decision:** Callers authenticate with HTTP Basic. Spring Security checks the credentials against users stored in the database, with passwords stored hashed.
- **Options considered:** No identity, a trusted `X-User-Id` header, HTTP Basic with Spring Security, token login (JWT).
- **Rationale:** Sharing rules mean nothing unless the service knows who is calling, which rules out no identity. A trusted header lets anyone claim to be anyone. HTTP Basic is real authentication with little code, and reviewers can test it with `curl -u user:pass`. JWT needs a login endpoint, token signing and expiry handling for little extra value here. Accepted tradeoffs: Spring Security adds configuration that has to be explained, and Basic sends credentials on every request, so real use needs HTTPS.
- **Decided by / date:** Steven Brown, 2026-11-11

## 014: A note belongs to a user, who shares it with teams

- **Decision:** Each note is owned by the user who wrote it. It is private until the owner shares it with a team.
- **Options considered:** Notes belong to one team; notes belong to a user who shares them with teams; per-note permissions for individual users.
- **Rationale:** The use case is that one user writes their notes and then shares them with a team. Team-owned notes cannot express a private note or a draft. Per-note permissions for individual users is more than several small teams need. Accepted tradeoff: an extra table linking notes to teams, and more permission cases to test than team-owned notes.
- **Decided by / date:** Steven Brown, 2026-11-11

## 015: Users and teams are managed through endpoints, without roles

- **Decision:** The service has endpoints to register users, create teams and add team members. Team members have no roles.
- **Options considered:** Seeded users and teams only; management endpoints; management endpoints plus team roles (admin and member).
- **Rationale:** With endpoints, reviewers can use the service end to end from an empty database, which seeded data does not allow. Roles are the better long-term model, but they add rules to build, test and defend, so they are listed in the README as a change to make with more time.
- **Decided by / date:** Steven Brown, 2026-11-11

## 016: Notes get edit conflict detection and paging; other features are deferred

- **Decision:** Beyond create, read, update and delete, notes get edit conflict detection (a save based on a stale copy is rejected instead of overwriting) and paged list endpoints. Text search, tags, edit history and soft delete are out of scope and listed in the README as nice to have with more time.
- **Options considered:** Edit conflict detection, paging, text search, tags, edit history, soft delete.
- **Rationale:** Silent overwrites are a correctness problem, and JPA makes detection cheap. Since decision 017 makes shared notes read-only for everyone but the owner, conflict detection currently guards the owner saving from two sessions at once; it becomes essential if team members are later allowed to edit. Paging is cheap with Spring Data and avoids unbounded responses. Each deferred feature adds schema and tests, and text search is where H2 and PostgreSQL differ most.
- **Decided by / date:** Steven Brown, 2026-11-11

## 017: Only the owner can change a note; team members read it

- **Decision:** A note shared with a team is read-only for that team's members. Only the owner can edit, delete, share or unshare it.
- **Options considered:** Team members can edit shared notes; team members can only read them.
- **Rationale:** This is the smallest rule set that delivers the use case for a first version: one user writes notes and shares them with a team. It keeps permission checks to two cases, owner or reader. Letting team members edit is listed in the README as a change to make with more time.
- **Decided by / date:** Steven Brown, 2026-11-11

## 018: A note can be shared with more than one team

- **Decision:** The owner can share one note with any number of teams.
- **Options considered:** One team per note; many teams per note.
- **Rationale:** The table linking notes to teams supports many teams at no extra cost, and limiting it to one would need an extra rule.
- **Decided by / date:** Steven Brown, 2026-11-11

## 019: A user can share a note with a team they are not a member of

- **Decision:** The owner does not have to belong to a team to share a note with it.
- **Options considered:** Owner must be a member of the team; owner can share with any team.
- **Rationale:** Sharing across teams is part of the intended use: someone on one team should be able to pass a note to another team. Accepted tradeoff: a team can receive notes from people outside it, and has no way to refuse them in this version.
- **Decided by / date:** Steven Brown, 2026-11-11

## 020: Any team member can add members; the creator is the first member

- **Decision:** The user who creates a team becomes its first member. Any existing member can add another user to the team.
- **Options considered:** Any member can add members; only the creator can add members; team roles (deferred by decision 015).
- **Rationale:** Without roles, this is the simplest rule that still stops outsiders from adding themselves to a team and reading its shared notes.
- **Decided by / date:** Steven Brown, 2026-11-11

## 021: User registration is open

- **Decision:** Anyone can register a user through an endpoint that needs no authentication.
- **Options considered:** Open registration; users seeded or created by an administrator.
- **Rationale:** Reviewers can start from an empty database and create their own users. Accepted tradeoff: nothing limits who signs up, which a real deployment would need to control.
- **Decided by / date:** Steven Brown, 2026-11-11

## 022: Schema has five tables

- **Decision:** The schema is:

  | Table | Columns |
  |---|---|
  | `users` | `id`, `username` (unique), `password_hash`, `created_at` |
  | `teams` | `id`, `name` (unique), `created_by` → users, `created_at` |
  | `team_members` | `team_id` → teams, `user_id` → users, `added_at`; primary key is the pair |
  | `notes` | `id`, `owner_id` → users, `title`, `body`, `version`, `created_at`, `updated_at` |
  | `note_shares` | `note_id` → notes, `team_id` → teams, `shared_at`; primary key is the pair |

  Passwords are stored as BCrypt hashes and never returned. Deleting a note removes its shares.
- **Options considered:** None recorded beyond this proposal.
- **Rationale:** One table per concept from decisions 013 to 021. The two linking tables use the pair of IDs as their primary key, so the database itself prevents a duplicate membership or share. `version` on notes carries edit conflict detection (decision 016).
- **Decided by / date:** Steven Brown, 2026-11-11

## 023: Endpoint list

- **Decision:** The service exposes these endpoints under `/api/v1` (decision 028). All need HTTP Basic credentials except registration.

  | Method and path | What it does | Who |
  |---|---|---|
  | `POST /users` | Register with a username and password | Anyone |
  | `GET /users/me` | Return the calling user | Any user |
  | `POST /teams` | Create a team; the caller becomes its first member | Any user |
  | `GET /teams` | List all teams, paged | Any user |
  | `GET /teams/{id}` | Team details and members | Members |
  | `POST /teams/{id}/members` | Add a user by username | Members |
  | `POST /notes` | Create a private note | Any user |
  | `GET /notes` | List notes the caller can see, paged; filter by owned, shared, or one team | Any user |
  | `GET /notes/{id}` | Read one note | Owner, or member of a team it is shared with |
  | `PUT /notes/{id}` | Replace title and body | Owner |
  | `DELETE /notes/{id}` | Delete the note and its shares | Owner |
  | `GET /notes/{id}/shares` | List the teams the note is shared with | Owner |
  | `PUT /notes/{id}/shares/{teamId}` | Share with a team; repeating it is harmless | Owner |
  | `DELETE /notes/{id}/shares/{teamId}` | Stop sharing with a team | Owner |

  Errors use Spring's problem-details JSON format: `400` for invalid input, `401` for missing or wrong credentials, `409` for a stale version or a taken username or team name.
- **Options considered:** Dropping `GET /users/me`, since any authenticated request checks credentials and registration returns the user ID. It was kept.
- **Rationale:** The list covers each rule in decisions 013 to 021 and nothing more. Sharing is modelled as a sub-resource of the note with `PUT` and `DELETE`, so sharing twice or unsharing twice is safe to repeat.
- **Decided by / date:** Steven Brown, 2026-11-11

## 024: IDs are UUIDs

- **Decision:** Every table's ID is a UUID.
- **Options considered:** UUIDs, auto-incrementing numbers.
- **Rationale:** UUIDs cannot be guessed or enumerated, which matters when IDs appear in URLs for private notes. They also avoid the ID generation differences between H2 and PostgreSQL. Accepted tradeoff: they are harder to read and type than numbers.
- **Decided by / date:** Steven Brown, 2026-11-11

## 025: Any user can list all teams

- **Decision:** `GET /teams` returns every team's name and ID to any authenticated user. Membership is only visible to members.
- **Options considered:** List all teams; list only the caller's teams.
- **Rationale:** Decision 019 lets a user share with a team they are not in, so they need a way to find it. Accepted tradeoff: team names are visible to every registered user.
- **Decided by / date:** Steven Brown, 2026-11-11

## 026: A note the caller cannot see returns 404; one they cannot change returns 403

- **Decision:** Requests for a note the caller has no access to get `404`. Attempts to change a note the caller can read but does not own get `403`.
- **Options considered:** `404` or `403` for a note the caller cannot see.
- **Rationale:** `404` does not reveal that a private note exists. `403` is accurate where the caller already knows the note exists.
- **Decided by / date:** Steven Brown, 2026-11-11

## 027: The note version is sent in the request body

- **Decision:** A client updating a note includes the `version` it read in the request body. A stale version gets `409`.
- **Options considered:** Version in the request body; version in an `If-Match` header.
- **Rationale:** A body field is the simplest to use and to explain. `If-Match` is the HTTP-standard mechanism but is less obvious to someone reading the API for the first time.
- **Decided by / date:** Steven Brown, 2026-11-11

## 028: Endpoints are versioned under /api/v1

- **Decision:** Every endpoint path starts with `/api/v1`. The service is not live yet, so everything is version 1.
- **Options considered:** Plain paths; a version prefix.
- **Rationale:** A version prefix leaves room to change the API later without breaking existing clients.
- **Decided by / date:** Steven Brown, 2026-11-11

## 029: Membership removal and account changes are deferred

- **Decision:** The first version has no endpoints to remove a team member, leave a team, change a password or delete a user. All four are listed in the README as changes to make with more time.
- **Options considered:** Include them now; defer them.
- **Rationale:** None of them is needed for the core use case of writing a note and sharing it. Accepted tradeoff: team membership can only grow, and accounts cannot be changed or removed.
- **Decided by / date:** Steven Brown, 2026-11-11

## 030: Field limits

- **Decision:** Usernames are 3 to 50 characters. Passwords are at least 8 characters. Note titles are required and up to 200 characters. Note bodies can be empty and are up to 10,000 characters.
- **Options considered:** None recorded beyond this proposal.
- **Rationale:** None recorded yet.
- **Decided by / date:** Steven Brown, 2026-11-11

## 031: Code is packaged by feature

- **Decision:** Under `com.bluestaq.notes`, code is grouped into feature packages (`user`, `team`, `note`), each holding its own controller, service, repository and entity.
- **Options considered:** Package by feature; package by layer (`controller`, `service`, `repository`, `model`).
- **Rationale:** Packaging by feature keeps related code together and makes the domain visible from the folder list. Packaging by layer is the more traditional Spring layout, but it spreads each feature across several packages.
- **Decided by / date:** Steven Brown, 2026-11-11

## 032: The plain IntelliJ project files are removed

- **Decision:** `Bluestaq.iml` and `src/Main.java` are deleted, and `.idea/` is no longer tracked in Git.
- **Options considered:** Keep them; remove them.
- **Rationale:** The Gradle build replaces them, and IntelliJ regenerates its own files when it imports the Gradle project.
- **Decided by / date:** Steven Brown, 2026-11-11

## 033: Health check uses Spring Boot Actuator

- **Decision:** The service has a health endpoint at `GET /actuator/health`, provided by Spring Boot Actuator. It needs no authentication, checks the database connection, and lists each component it checked with its status. It returns `200` when everything is up and `503` when something is down.
- **Options considered:** Spring Boot Actuator; a hand-written controller at `/api/v1/health`.
- **Rationale:** Actuator is built in and widely recognised, and its health check already tests the database, so there is almost no code to write or maintain. Accepted tradeoffs: one more dependency, and the path sits outside the `/api/v1` prefix used by the other endpoints (decision 028). Only the health endpoint is exposed over HTTP, which is Actuator's default.
- **Decided by / date:** Steven Brown, 2026-11-11

## 034: API documentation is Swagger UI through springdoc-openapi

- **Decision:** The API is described by springdoc-openapi 3.2.0 and shown in Swagger UI at `/swagger-ui.html`. The documentation pages and the OpenAPI description at `/v3/api-docs` need no authentication. HTTP Basic is declared as the default security scheme, so the page has an Authorize button. Endpoint documentation is updated in the same commit as the endpoint (rule 4 in `CLAUDE.md`).
- **Options considered:** Swagger UI, Scalar, Redoc, Spring REST Docs, an `.http` request file.
- **Rationale:** Swagger UI is the page reviewers are most likely to recognise, it can send real requests, and it comes with springdoc as one dependency. Scalar looks more modern but is less familiar. Redoc is read-only. Spring REST Docs produces static pages and needs much more setup. The documentation is open so a reviewer can load it before they have an account. Accepted tradeoff: anyone who can reach the service can read the full API description.
- **Decided by / date:** Steven Brown, 2026-11-11