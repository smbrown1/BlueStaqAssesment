# BlueStaqAssesment

Backend for a note-taking service shared among several small teams. Built with Java 25, Spring Boot and Gradle. The task is described in `Requirements.md`, and every design decision is logged in `DECISIONS.md`.

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

## What I'd change with more time

- **Move to PostgreSQL.** H2 was chosen to make the service easy to run and review, not because it is the right database for real use. With more time I would run the service on PostgreSQL, provide it through Docker Compose, and run the tests against it so that differences between the two databases are caught.
- **Add team roles.** Team members currently have no roles. With more time I would add team admins and members, so that only admins can change membership or remove a team.
- **Let team members edit shared notes.** A shared note is currently read-only for everyone but its owner. With more time I would let the owner grant edit access to a team.

Nice to have:

- **Text search** across note titles and bodies.
- **Tags** for organising notes.
- **Edit history**, so earlier versions of a note can be viewed and restored.
- **Soft delete**, so a deleted note can be recovered.