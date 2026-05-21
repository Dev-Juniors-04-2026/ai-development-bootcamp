# CLAUDE.md

Project-specific notes for working in this repo. See `PRD.md` for the product spec and `ARCHITECTURE.md` for layer structure and file responsibilities.

## What this is

Single-page in-browser TODO app. Plain HTML/CSS/JS frontend (no build step). Spring Boot 4.x + Java 21 backend, PostgreSQL, Flyway, hexagonal architecture. JWT auth implemented; OAuth2 (GitHub) is the next step.

## Commands

Frontend lives at the repo root; backend in `backend/`.

| Task | Command |
|---|---|
| Run unit tests (JS) | `npm test` |
| Run BDD/Cucumber tests | `npm run test:bdd` |
| Run backend unit + integration tests | `cd backend && ./mvnw test` |
| Run a single backend test | `cd backend && ./mvnw test -Dtest=ClassName#method` |
| Build backend jar | `cd backend && ./mvnw package` |
| Run full stack locally | `docker compose up --build` (backend on `:8080`, db on `:5432`) |
| Serve frontend | open `index.html` directly, or any static server on the repo root |
| Reset test DB | `DELETE /api/todos/reset` (test profile only) |

JWT secret in dev falls back to an insecure default. Override with `JWT_SECRET=$(openssl rand -base64 32) docker compose up`.

## Repo layout (high level)

- `index.html`, `css/`, `src/` — frontend (`app.js`, `api.js`, `auth.js`)
- `backend/src/main/java/com/example/todoapp/` — hexagonal Spring Boot app: `domain/`, `application/`, `adapter/in/http/`, `adapter/out/persistence/`, `adapter/out/security/`
- `features/` — Cucumber `.feature` files and `step_definitions/`
- `backend/src/test/` — JUnit tests, mirroring main package structure

## Conventions

**Hexagonal boundaries (enforce strictly):**
- `domain/` must not import Spring, JPA, Jackson, or anything from `adapter/`. Pure Java only.
- `application/` may depend on `domain/` ports, nothing else.
- `adapter/in/http/` and `adapter/out/persistence/` depend inward on ports; never on each other.
- New persistence → add a port in `domain/port/out/`, implement it in `adapter/out/persistence/`.
- New HTTP endpoint → add a port in `domain/port/in/`, implement in `application/`, expose via `adapter/in/http/`.

**HTTP status codes:** map domain exceptions in `GlobalExceptionHandler`. Auth failures are `401`, not `403` or `500` — a past bug shipped `500` on bad login and broke the frontend's error display (see commit `7b640d3`).

**Cucumber step definitions:** use `assert.strictEqual` (and friends) — not `throw new Error(...)`. All `*.steps.js` files follow this.

**DTOs:** request DTOs end in `Request`, response DTOs end in `Response`. Validation lives on the DTO via Bean Validation (`@NotBlank`, `@ValidIsoDate`, etc.) — not in controllers or use cases.

**Passwords:** hashed with Argon2 via `PasswordHasher` port. Never log or return password fields.

## Gotchas

- Frontend has **no build step**. Don't add bundlers, frameworks, or npm-runtime deps. `package.json` is test-tooling only.
- Maven dependency download can be slow on a cold deployment pipeline (see commit `1c41821`). If CI flakes early, suspect the download timeout before suspecting the code.
- Tests use H2 by default and Testcontainers for the integration layer; production uses PostgreSQL. Schema differences will bite — keep migrations Flyway-compatible across both.

## Out of scope (do not propose)

Cloud sync, native apps, priorities, tags, drag-and-drop reordering. See PRD §3 and §10.
