# Architecture

## TODO App — v2.0

---

## Overview

Single-page, in-browser TODO application with a Spring Boot REST backend and PostgreSQL database. The frontend is plain HTML/CSS/JS (no build tools, no frameworks). State is persisted via the backend API; `localStorage` is no longer the primary storage.

> **Integration status:** The backend supports listing, creating, toggling, and deleting todos. The frontend calls the backend API via `api.js`. Auth (register/login/JWT) is planned but not yet implemented in either layer.

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
│   └── auth.js         # Login/register/logout UI and token storage  [planned]
└── backend/            # Spring Boot application (Java 21, Maven)
    └── src/main/java/com/example/todoapp/
        ├── domain/
        │   ├── model/
        │   │   ├── Todo.java
        │   │   └── User.java                    [planned]
        │   └── port/
        │       ├── in/
        │       │   ├── TodoUseCase.java
        │       │   └── UserUseCase.java          [planned]
        │       └── out/
        │           ├── TodoRepository.java
        │           └── UserRepository.java       [planned]
        ├── application/
        │   ├── TodoUseCaseImpl.java
        │   └── UserUseCaseImpl.java              [planned]
        └── adapter/
            ├── in/http/
            │   ├── TodoController.java
            │   ├── AuthController.java           [planned]
            │   ├── CreateTodoRequest.java
            │   ├── RegisterRequest.java          [planned]
            │   ├── LoginRequest.java             [planned]
            │   ├── TodoResponse.java
            │   ├── TokenResponse.java            [planned]
            │   ├── JwtFilter.java                [planned]
            │   ├── GlobalExceptionHandler.java
            │   └── TestResetController.java
            └── out/persistence/
                ├── TodoJpaEntity.java
                ├── TodoJpaRepository.java
                ├── TodoPersistenceAdapter.java
                ├── UserJpaEntity.java            [planned]
                ├── UserJpaRepository.java        [planned]
                └── UserPersistenceAdapter.java   [planned]
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
| `User` | Domain model: `id` (UUID), `username` (String), `passwordHash` (String) — **[planned]** |
| `UserUseCase` | Inbound port — `register(username, password)`, `login(username, password): token` — **[planned]** |
| `UserRepository` | Outbound port — `save()`, `findByUsername()` — **[planned]** |

### Application (`application/`)

| Class | Description |
|---|---|
| `TodoUseCaseImpl` | Implements `TodoUseCase`; orchestrates domain logic via `TodoRepository` |
| `UserUseCaseImpl` | Implements `UserUseCase`; hashes passwords with **Argon2**, issues JWT tokens — **[planned]** |

### Adapters

**Inbound (`adapter/in/http/`)**

| Class | Description |
|---|---|
| `TodoController` | `GET /api/todos`, `POST /api/todos`, `PATCH /api/todos/{id}`, `DELETE /api/todos/{id}` |
| `AuthController` | `POST /api/auth/register`, `POST /api/auth/login` — **[planned]** |
| `CreateTodoRequest` | Request DTO: `{ title: String, dueDate: String\|null }` |
| `RegisterRequest` | Request DTO: `{ username: String, password: String }` — **[planned]** |
| `LoginRequest` | Request DTO: `{ username: String, password: String }` — **[planned]** |
| `TodoResponse` | Response DTO: `{ id: UUID, title: String, completed: boolean, dueDate: String\|null }` |
| `TokenResponse` | Response DTO: `{ token: String }` — **[planned]** |
| `JwtFilter` | Validates `Authorization: Bearer <token>` on every request — **[planned]** |
| `GlobalExceptionHandler` | Maps domain exceptions to HTTP error responses |
| `TestResetController` | `DELETE /api/todos/reset` — test profile only, clears all data |

**Outbound (`adapter/out/persistence/`)**

| Class | Description |
|---|---|
| `TodoPersistenceAdapter` | Implements `TodoRepository` using Spring Data JPA |
| `TodoJpaEntity` | JPA entity mapped to `todo` table |
| `TodoJpaRepository` | Spring Data `JpaRepository` |
| `UserPersistenceAdapter` | Implements `UserRepository` using Spring Data JPA — **[planned]** |
| `UserJpaEntity` | JPA entity mapped to `user` table — **[planned]** |
| `UserJpaRepository` | Spring Data `JpaRepository` — **[planned]** |

### REST API

| Method | Path | Status | Description |
|---|---|---|---|
| `POST` | `/api/auth/register` | ❌ planned | Register a new user; body: `{ "username": "...", "password": "..." }` |
| `POST` | `/api/auth/login` | ❌ planned | Login; returns `{ "token": "..." }` |
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
auth.js — check localStorage for JWT token    [planned]
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

## Constraints (from PRD)

- Frontend: Plain HTML/CSS/JS only — no npm, no bundler, no framework
- Backend: Spring Boot 4.x, Java 21, Maven, PostgreSQL
- No cloud sync, no native apps, no priorities/tags
