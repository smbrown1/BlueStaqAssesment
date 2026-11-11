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
- **Rationale:** Notes shared with a team can be edited by more than one person, so silent overwrites are a correctness problem, and JPA makes detection cheap. Paging is cheap with Spring Data and avoids unbounded responses. Each deferred feature adds schema and tests, and text search is where H2 and PostgreSQL differ most.
- **Decided by / date:** Steven Brown, 2026-11-11