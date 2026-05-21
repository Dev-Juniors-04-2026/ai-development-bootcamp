# Architecture

## TODO App — v2.0

---

## Overview

Single-page, in-browser TODO application with a Spring Boot REST backend and PostgreSQL database. The frontend is plain HTML/CSS/JS (no build tools, no frameworks). State is persisted via the backend API; `localStorage` is no longer the primary storage.

> **Integration status:** The backend supports listing, creating, toggling, and deleting todos, as well as user registration and login with JWT-based authentication. The frontend calls the backend API via `api.js` and handles login/register/logout via `auth.js`. OAuth2 login (e.g., GitHub) is planned as the next step.

---

## System Overview

```
Browser (HTML/CSS/JS)
        │
        │  HTTP (REST)
        ▼
Spring Boot Backend  (/api/todos)
        │
        │  JPA
        ▼
PostgreSQL Database
```

---

## File Structure

```
/
├── index.html          # Page structure and DOM skeleton
├── css/
│   └── style.css       # All visual styling
├── src/
│   ├── app.js          # Event wiring, DOM rendering, API calls for todos
│   ├── api.js          # fetch wrappers for all backend endpoints
│   └── auth.js         # Login/register/logout UI and token storage
└── backend/            # Spring Boot application (Java 21, Maven)
    └── src/main/java/com/example/todoapp/
        ├── domain/
        │   ├── InvalidCredentialsException.java
        │   ├── UsernameAlreadyTakenException.java
        │   ├── model/
        │   │   ├── Todo.java
        │   │   ├── User.java
        │   │   └── AuthenticatedUser.java
        │   └── port/
        │       ├── in/
        │       │   ├── TodoUseCase.java
        │       │   └── UserUseCase.java
        │       └── out/
        │           ├── TodoRepository.java
        │           ├── UserRepository.java
        │           └── PasswordHasher.java
        ├── application/
        │   ├── TodoUseCaseImpl.java
        │   └── UserUseCaseImpl.java
        └── adapter/
            ├── in/http/
            │   ├── TodoController.java
            │   ├── AuthController.java
            │   ├── CreateTodoRequest.java
            │   ├── RegisterRequest.java
            │   ├── LoginRequest.java
            │   ├── TodoResponse.java
            │   ├── TokenResponse.java
            │   ├── JwtFilter.java
            │   ├── JwtService.java
            │   ├── SecurityConfig.java
            │   ├── ValidIsoDate.java
            │   ├── ValidIsoDateValidator.java
            │   ├── GlobalExceptionHandler.java
            │   └── TestResetController.java
            └── out/
                ├── persistence/
                │   ├── TodoJpaEntity.java
                │   ├── TodoJpaRepository.java
                │   ├── TodoPersistenceAdapter.java
                │   ├── UserJpaEntity.java
                │   ├── UserJpaRepository.java
                │   └── UserPersistenceAdapter.java
                └── security/
                    └── Argon2PasswordHasher.java
```

---

## index.html

Responsibilities:
- Defines the page skeleton (head, body, meta)
- Links `css/style.css` and `src/app.js`
- Contains the static layout: header, input row, todo list container, empty-state message

Key elements:
- `<input id="todo-input">` — task entry field
- `<button id="add-btn">` — triggers add action
- `<ul id="todo-list">` — dynamic list; `app.js` renders items here
- `<p id="empty-state">` — shown when list is empty

---

## style.css

Responsibilities:
- Layout (centered single column, responsive width)
- Input row and button appearance
- Todo item layout (checkbox left, title center, delete button right)
- Completed-item style (strikethrough text, muted color)
- Empty-state visibility toggle via `.hidden` utility class

No external fonts or icon libraries. Delete button uses a plain `✕` character.

---

## api.js

Responsibilities:
- Encapsulates all HTTP calls to the backend
- Keeps fetch details out of `app.js`

| Export | Description |
|---|---|
| `fetchTodos()` | `GET /api/todos` — returns array of todo objects |
| `createTodo(title, dueDate)` | `POST /api/todos` — creates and returns the new todo |
| `toggleTodo(id)` | `PATCH /api/todos/{id}` — flips completed state, returns updated todo |
| `deleteTodo(id)` | `DELETE /api/todos/{id}` |

---

## app.js

Responsibilities:
- On startup calls `fetchTodos()` and renders the full list
- Handles all user events (add, toggle, delete) via `api.js`, then re-renders

### Functions

| Function | Description |
|---|---|
| `render(todos)` | Clears and rebuilds `#todo-list` from a todos array; toggles empty-state |
| `refresh()` | Calls `fetchTodos()` and passes result to `render()` |
| `handleAdd()` | Reads input, calls `createTodo()`, then `refresh()` |

### Event wiring

- `#add-btn` click → `handleAdd`
- `#todo-input` keydown `Enter` → `handleAdd`
- Delegated `change` on `#todo-list` checkbox → `toggleTodo`, then `refresh`
- Delegated `click` on `.delete-btn` → `deleteTodo`, then `refresh`

Input is trimmed before use; empty/whitespace submissions are ignored.

---

## Backend — Hexagonal Architecture

The backend follows the **Ports & Adapters (Hexagonal) pattern**: business logic in the domain is fully isolated from infrastructure concerns.

### Layers

```
┌──────────────────────────────────────────────────────────────────┐
│  Adapter (in)          Application           Adapter (out)        │
│  TodoController  →→  TodoUseCaseImpl  →→  TodoPersistenceAdapter │
│  (HTTP/REST)          uses ports             (JPA/PostgreSQL)     │
└──────────────────────────────────────────────────────────────────┘
                          ↕ domain ports
                     ┌─────────────────────┐
                     │       Domain        │
                     │  Todo               │
                     │  TodoUseCase (in)   │
                     │  TodoRepository (out)│
                     └─────────────────────┘
```

### Domain (`domain/`)

| Class | Description |
|---|---|
| `Todo` | Domain model: `id` (UUID), `title` (String), `completed` (boolean), `dueDate` (String\|null), `userId` (UUID) |
| `TodoUseCase` | Inbound port — `getAll()`, `create(title, dueDate)`, `toggle(id)`, `delete(id)` |
| `TodoRepository` | Outbound port — `save()`, `findAll()`, `findById()`, `delete()`, `deleteAll()` |
| `User` | Domain model: `id` (UUID), `username` (String), `passwordHash` (String) |
| `AuthenticatedUser` | Domain model representing a logged-in user resolved from a JWT |
| `UserUseCase` | Inbound port — `register(username, password)`, `login(username, password): token` |
| `UserRepository` | Outbound port — `save()`, `findByUsername()` |
| `PasswordHasher` | Outbound port — `hash()`, `verify()` |

### Application (`application/`)

| Class | Description |
|---|---|
| `TodoUseCaseImpl` | Implements `TodoUseCase`; orchestrates domain logic via `TodoRepository` |
| `UserUseCaseImpl` | Implements `UserUseCase`; hashes passwords with **Argon2**, issues JWT tokens |

### Adapters

**Inbound (`adapter/in/http/`)**

| Class | Description |
|---|---|
| `TodoController` | `GET /api/todos`, `POST /api/todos`, `PATCH /api/todos/{id}`, `DELETE /api/todos/{id}` |
| `AuthController` | `POST /api/auth/register`, `POST /api/auth/login` |
| `CreateTodoRequest` | Request DTO: `{ title: String, dueDate: String\|null }` |
| `RegisterRequest` | Request DTO: `{ username: String, password: String }` |
| `LoginRequest` | Request DTO: `{ username: String, password: String }` |
| `TodoResponse` | Response DTO: `{ id: UUID, title: String, completed: boolean, dueDate: String\|null }` |
| `TokenResponse` | Response DTO: `{ token: String }` |
| `JwtFilter` | Validates `Authorization: Bearer <token>` on every request and binds the `AuthenticatedUser` to the request context |
| `JwtService` | Issues and validates JWT tokens (signing, expiry, claims) |
| `SecurityConfig` | Spring Security configuration: stateless sessions, JWT filter wiring, public vs. authenticated routes |
| `ValidIsoDate` / `ValidIsoDateValidator` | Bean Validation constraint for ISO 8601 date strings on request DTOs |
| `GlobalExceptionHandler` | Maps domain exceptions to HTTP error responses |
| `TestResetController` | `DELETE /api/todos/reset` — test profile only, clears all data |

**Outbound (`adapter/out/persistence/`)**

| Class | Description |
|---|---|
| `TodoPersistenceAdapter` | Implements `TodoRepository` using Spring Data JPA |
| `TodoJpaEntity` | JPA entity mapped to `todo` table |
| `TodoJpaRepository` | Spring Data `JpaRepository` |
| `UserPersistenceAdapter` | Implements `UserRepository` using Spring Data JPA |
| `UserJpaEntity` | JPA entity mapped to `user` table |
| `UserJpaRepository` | Spring Data `JpaRepository` |
| `Argon2PasswordHasher` | Implements `PasswordHasher` using Argon2 |

### REST API

| Method | Path | Status | Description |
|---|---|---|---|
| `POST` | `/api/auth/register` | ✅ implemented | Register a new user; body: `{ "username": "...", "password": "..." }` |
| `POST` | `/api/auth/login` | ✅ implemented | Login; returns `{ "token": "..." }` |
| `GET` | `/api/todos` | ✅ implemented | Returns all todos for the authenticated user |
| `POST` | `/api/todos` | ✅ implemented | Creates a new todo; body: `{ "title": "...", "dueDate": "..." }` |
| `PATCH` | `/api/todos/{id}` | ✅ implemented | Toggle completed state |
| `DELETE` | `/api/todos/{id}` | ✅ implemented | Delete a single todo |
| `DELETE` | `/api/todos/reset` | ✅ test only | Deletes all todos (test profile only) |

### Tech Stack

| Attribute | Details |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 4.x |
| Persistence | Spring Data JPA + PostgreSQL |
| DB migrations | Flyway |
| Build | Maven |
| Auth | JWT (Bearer tokens); password hashing via Argon2 |
| Test DB | H2 (in-memory), Testcontainers (integration) |

---

## Data Flow

```
Page load
    │
    ▼
auth.js — check localStorage for JWT token
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
    │
    ▼
Updated UI
```

---

## Planned — OAuth2 Login

Next step: allow users to log in with a third-party provider (e.g., GitHub) via OAuth2 Authorization Code flow. The backend will accept the provider callback, resolve/create a local `User`, and issue the same JWT used by password login. Frontend `auth.js` will gain a "Log in with GitHub" button that redirects to the provider and handles the post-callback token storage.

---

## Constraints (from PRD)

- Frontend: Plain HTML/CSS/JS only — no npm, no bundler, no framework
- Backend: Spring Boot 4.x, Java 21, Maven, PostgreSQL
- No cloud sync, no native apps, no priorities/tags
