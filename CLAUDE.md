# Project Rules for Claude

These rules apply to all work in this repository. The task itself is defined in `Requirements.md`.

## 1. Commit frequently, with explanations

- Commit after each small, self-contained unit of work (one passing test and its implementation, one refactor, one doc update), not in large batches.
- Every commit message has a short summary line and a body explaining **what** changed and **why**.
- Never mix unrelated changes in one commit.
- Do not push unless asked.

## 2. Red to green test-driven development

- Write the test before the implementation.
- **Red:** write one small, focused test for the next piece of behavior, run it, and confirm it fails for the expected reason. Show the failing run.
- **Green:** write the minimum code needed to make it pass. Show the passing run.
- **Refactor:** clean up code and tests while everything stays green.
- Bug fixes start with a test that reproduces the bug.
- If TDD is not practical for a task (config, scaffolding, exploratory spikes), say so before starting and add tests afterward where it makes sense.

## 3. Never assume a design decision; always get human approval

- Do not choose a language, framework, library, schema, endpoint shape, storage option, project structure, scope boundary, or any other design-level detail on your own.
- When a decision is needed, stop and present the options with their tradeoffs and a recommendation, then wait for approval before acting.
- If something is ambiguous in `Requirements.md`, ask rather than interpret.
- Once a decision is approved, record it in `DECISIONS.md` in the same commit as (or immediately before) the work that depends on it.

## 4. Keep the Swagger documentation in step with the endpoints

The API is documented with springdoc-openapi and shown in Swagger UI at `/swagger-ui.html`. Whenever an endpoint is added, changed or removed, update its documentation in the same commit:

- Give each controller a `@Tag` and each endpoint an `@Operation` summary.
- Document every status code the endpoint can return with `@ApiResponse`, including the error cases (`400`, `401`, `403`, `404`, `409` as they apply).
- Describe request and response fields with `@Schema` where the name alone is not clear, and make sure validation limits show up in the description.
- HTTP Basic is required by default (set in `OpenApiConfig`). For an endpoint that needs no credentials, add an empty `@SecurityRequirements` annotation so Swagger UI does not show a lock on it, and permit its path in `SecurityConfig`.
- Add or update a test that checks the endpoint appears in `/v3/api-docs` as intended.
- If the general setup changes (title, version, security scheme, documentation paths), update `OpenApiConfig`, `SecurityConfig` and `ApiDocsTests` together.

## 5. Follow the plan, keep it current, and stop at checkpoints

- `PLAN.md` sets the order of work. Build the steps in that order and do not start a step that is not in it.
- When a step is finished, tick it off, update "Current position" and add a line to the progress log, in the same commit as the work.
- Stop at every checkpoint in the plan and wait for human approval before starting the next phase. Report what was built, the test results, anything that differs from the plan, and what comes next.
- Having a plan does not relax rule 3. Stop in the middle of a phase for any design question that `DECISIONS.md` does not already answer.
- Changing the plan itself (adding, dropping or reordering steps) needs approval first.