# Architecture

> Structure, boundaries, and data flow. For the product spec see `PRD.md`; for commands and conventions see `CLAUDE.md`. Field-level data definitions live in PRD §7.

---

## Overview

Single-page, in-browser TODO application with a Spring Boot REST backend and PostgreSQL database. Frontend is plain HTML/CSS/JS (no build tools, no frameworks). State is persisted via the backend API; `localStorage` holds only the JWT token.

**Integration status:** Todos (list/create/toggle/delete) and auth (register/login with JWT) are shipped end-to-end. OAuth2 third-party login is the next step — see "Planned" below.

---

## System Overview

```
Browser (HTML/CSS/JS)
        │
        │  HTTP (REST + Bearer token)
        ▼
Spring Boot Backend  (/api/auth/*, /api/todos/*)
        │
        │  JPA / Flyway
        ▼
PostgreSQL Database
```

---

## File Structure

```
/
├── index.html
├── css/style.css
├── src/                          # frontend
│   ├── app.js                    # todo UI: event wiring, rendering
│   ├── api.js                    # fetch wrappers for backend endpoints
│   └── auth.js                   # login/register/logout UI and token storage
├── features/                     # Cucumber BDD tests (.feature + step_definitions/)
└── backend/                      # Spring Boot, Java 21, Maven
    └── src/
        ├── main/
        │   ├── java/com/example/todoapp/
        │   │   ├── domain/             # pure Java, no Spring
        │   │   │   ├── model/          # Todo, User, AuthenticatedUser
        │   │   │   └── port/{in,out}/  # use-case + repository interfaces
        │   │   ├── application/        # use-case implementations
        │   │   └── adapter/
        │   │       ├── in/http/        # controllers, DTOs, JWT filter, SecurityConfig
        │   │       └── out/
        │   │           ├── persistence/    # JPA entities + adapters
        │   │           └── security/       # Argon2PasswordHasher
        │   └── resources/
        │       └── db/migration/       # Flyway migrations (V1__…, V2__…)
        └── test/
            └── java/…                  # JUnit unit + integration tests, mirrors main/
```

For per-file responsibilities and exported functions, read the source. This document does not duplicate them.

---

## Frontend

Three modules:

- **`api.js`** — fetch wrappers for the backend. All HTTP details live here; nothing else in the frontend calls `fetch` directly.
- **`app.js`** — wires DOM events to `api.js` calls and re-renders the todo list. Trims input and ignores empty submissions.
- **`auth.js`** — login/register/logout UI and JWT storage. Reads/writes the token in `localStorage` and toggles between the auth view and the todo view.

The HTML is a static skeleton; `index.html` and `style.css` carry no logic.

---

## Backend — Hexagonal Architecture

The backend follows **Ports & Adapters**: the domain is fully isolated from Spring, JPA, and HTTP concerns.

```
┌──────────────────────────────────────────────────────────────────┐
│  Adapter (in)          Application           Adapter (out)        │
│  *Controller     →→   *UseCaseImpl    →→   *PersistenceAdapter   │
│  (HTTP/REST)           uses ports            (JPA/PostgreSQL)     │
└──────────────────────────────────────────────────────────────────┘
                          ↕ domain ports
                  ┌─────────────────────────┐
                  │         Domain          │
                  │  Todo, User             │
                  │  *UseCase  (inbound)    │
                  │  *Repository (outbound) │
                  └─────────────────────────┘
```

**Boundary rules (enforced):**
- `domain/` must not import Spring, JPA, Jackson, or anything from `adapter/`.
- `application/` depends on `domain/` ports only.
- Adapters depend inward on ports, never on each other.

### Domain

- **Models:** `Todo`, `User`, `AuthenticatedUser`
- **Inbound ports:** `TodoUseCase`, `UserUseCase`
- **Outbound ports:** `TodoRepository`, `UserRepository`, `PasswordHasher`
- **Exceptions:** `InvalidCredentialsException`, `UsernameAlreadyTakenException` — mapped to HTTP by `GlobalExceptionHandler`

### Application

- `TodoUseCaseImpl` — orchestrates todo CRUD via `TodoRepository`
- `UserUseCaseImpl` — registers users (hashes via `PasswordHasher`) and authenticates (issues JWTs via `JwtService`)

### Inbound adapters (`adapter/in/http/`)

- `TodoController`, `AuthController` — REST endpoints (see table below)
- Request/response DTOs (`*Request`, `*Response`) with Bean Validation
- `JwtFilter` + `JwtService` — token validation and issuance
- `SecurityConfig` — Spring Security: stateless sessions, public vs. authenticated routes
- `ValidIsoDate` / `ValidIsoDateValidator` — custom Bean Validation constraint
- `GlobalExceptionHandler` — maps domain exceptions to HTTP responses
- `TestResetController` — `DELETE /api/todos/reset`, test profile only

### Outbound adapters

- `adapter/out/persistence/` — `Todo`/`User` JPA entities, Spring Data repositories, persistence adapters
- `adapter/out/security/` — `Argon2PasswordHasher`

### REST API

| Method | Path | Description |
|---|---|---|
| `POST` | `/api/auth/register` | Register; body `{ username, password }` |
| `POST` | `/api/auth/login` | Login; returns `{ token }` |
| `GET` | `/api/todos` | List todos for the authenticated user |
| `POST` | `/api/todos` | Create a todo |
| `PATCH` | `/api/todos/{id}` | Toggle completed state |
| `DELETE` | `/api/todos/{id}` | Delete a todo |
| `DELETE` | `/api/todos/reset` | Test profile only — wipe all data |

All `/api/todos/*` endpoints require `Authorization: Bearer <token>`.

### Tech Stack

| Attribute | Details |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 4.x |
| Persistence | Spring Data JPA + PostgreSQL |
| DB migrations | Flyway (`backend/src/main/resources/db/migration/V*__*.sql`) |
| Build | Maven |
| Auth | JWT Bearer tokens; Argon2 password hashing |
| Test DB | H2 (in-memory) for unit slices; Testcontainers (PostgreSQL) for integration |

---

## Data Flow

```
Page load
    │
    ▼
auth.js — check localStorage for JWT
    ├── no token  → show login/register UI
    └── has token → show todo UI
                        │
                        ▼
                  GET /api/todos  (Authorization: Bearer <token>)
                        │
                        ▼
                  render() — initial UI

User action (todo UI)
    │
    ▼
Event handler (app.js)
    │
    ├── add:    POST   /api/todos
    ├── toggle: PATCH  /api/todos/{id}
    └── delete: DELETE /api/todos/{id}
    │
    ▼
GET /api/todos → render() — rebuild DOM
```

---

## Planned — OAuth2 Login

Next step: allow login via a third-party provider (GitHub first) using the OAuth2 Authorization Code flow. Open design questions to resolve before implementation:

- Provider integration library (Spring Security OAuth2 client vs. raw flow)
- Linking model: does a GitHub user create a `User` row with a null `passwordHash`, or do we introduce a separate identity table that points to `User`?
- New endpoints: callback URL, account-linking flow
- Token issuance: same `JwtService` used by password login, so downstream `/api/todos/*` is unchanged

This section will be expanded once the design is settled.
