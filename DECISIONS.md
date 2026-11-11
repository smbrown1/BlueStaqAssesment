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

- **Decision:** Use the newest stable releases that work together. As checked on 2026-11-11: Spring Boot 4.1.2 (4.2.0 is still a release candidate) and Gradle 9.9.1. The Java version is still open, see the note below.
- **Options considered:** Latest stable releases, or older maintained lines (Spring Boot 4.0.x).
- **Rationale:** None recorded yet.
- **Decided by / date:** Steven Brown, 2026-11-11
- **Open:** Java version. Spring Boot 4.1.2 supports Java 17, 21, 25 and 27. Java 27 is the newest, Java 25 is the newest long-term-support release, and the only JDK installed on the development machine is 26.