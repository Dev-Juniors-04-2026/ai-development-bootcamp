# Product Requirements Document (PRD)
## TODO App

**Version:** 2.2
**Date:** 2026-05-21
**Status:** Draft

---

## 1. Overview

A simple, lightweight web-based TODO application that allows users to manage their daily tasks directly in the browser. The focus is on core functionality with no unnecessary complexity.

---

## 2. Goals

- Give users a fast and frictionless way to manage tasks in the browser
- Keep the UI minimal and intuitive — no onboarding required
- Deliver a working MVP with core CRUD operations for TODOs

---

## 3. Target Users

Anyone who needs a quick, no-setup task list in their browser — students, professionals, or casual users.

---

## 4. Platform

| Attribute | Details |
|-----------|---------|
| Platform  | Web (browser) + REST backend |
| Frontend  | Plain HTML/CSS/JS, no framework, no build tools |
| Backend   | Spring Boot (Java 21), REST API at `/api/todos` and `/api/auth/*` |
| Storage   | PostgreSQL via backend API. `localStorage` is used only to hold the JWT token on the frontend. |
| Auth      | JWT Bearer tokens; passwords hashed server-side with Argon2 |

---

## 5. Features & Requirements

### 5.0 User Accounts & Authentication

- A user can **register** with a username and password
- A user can **log in** with their username and password and gains access to their personal TODO list
- A user can **log out**, returning the app to the login screen
- Each user sees only their own TODOs; todos are scoped per account
- The login/register screen is shown when the user is not authenticated; the main TODO UI is shown after login
- Unauthenticated access to the TODO list is not permitted

**Password handling:**
- Must not be blank; no other constraints enforced today
- Stored hashed (Argon2); never returned by any API

**Error scenarios the UI must handle:**
- Registering with a username that already exists
- Registering or logging in with blank fields
- Logging in with wrong credentials (response must not reveal whether the username or the password was wrong)
- Making an authenticated request with an expired or invalid token (frontend should return the user to the login screen)
- Network failure on any auth request

Each of the above must produce a clear user-visible message. Exact wording and HTTP status codes are an implementation decision, not a product requirement — see `ARCHITECTURE.md` and `GlobalExceptionHandler` for the current mapping.

---

### 5.1 View TODO List

- The app displays a list of all current TODOs on the main screen
- Each TODO item shows its title and its completion status (checked / unchecked)
- If no TODOs exist, an empty state message is shown (e.g. *"No tasks yet. Add one above!"*)

### 5.2 Add a TODO

- A text input field and an "Add" button are always visible at the top of the list
- The user types a task title and submits via the "Add" button or by pressing `Enter`
- The new TODO appears immediately at the bottom of the list
- The input field is cleared after submission
- Empty or whitespace-only submissions are ignored (no TODO is created)

### 5.3 Check Off a TODO (Mark as Complete)

- Each TODO has a checkbox on its left side
- Clicking the checkbox toggles the TODO between **complete** and **incomplete**
- Completed TODOs are visually distinguished (e.g. strikethrough text, muted color)
- The toggle is reversible at any time

### 5.4 Remove a TODO

- Each TODO has a delete button (e.g. a trash icon or "✕") on its right side
- Clicking the delete button removes the TODO from the list immediately
- No confirmation dialog is required for MVP

### 5.5 Due Date

- Each TODO may optionally have a due date
- A date input is shown alongside the title input when adding a new TODO
- The date input accepts any ISO 8601 date, past or future. No timezone — date only.
- If a due date is set, it is displayed on the TODO item (e.g. "Due: Apr 28")
- If no due date is set, nothing is shown in its place
- Due dates are for display only — no sorting, filtering, or reminders

---

## 6. User Interface

### Login / Register screen (shown when not authenticated)

```
┌────────────────────────────────────────────────┐
│                 📝 TODO App                    │
├────────────────────────────────────────────────┤
│  [ Username ]                                  │
│  [ Password ]                                  │
│  [ Log in ]  [ Register ]                      │
└────────────────────────────────────────────────┘
```

### Main TODO screen (shown after login)

```
┌────────────────────────────────────────────────┐
│           📝 TODO App      [ Log out ]         │
├────────────────────────────────────────────────┤
│  [ What needs to be done? ]  [Due date]  [Add] │
├────────────────────────────────────────────────┤
│  ☐  Buy groceries           Due: Apr 28    [✕] │
│  ☑  Send project report                    [✕] │
│  ☐  Call the dentist        Due: Apr 30    [✕] │
└────────────────────────────────────────────────┘
```

### UI Rules

- Single-page layout; auth and todo views toggled by JS (no page navigation)
- Responsive width — readable on standard desktop browser windows
- No external design system required; plain CSS is acceptable

---

## 7. Data Model

**User** (persisted via backend):

| Field      | Type   | Description                        |
|------------|--------|------------------------------------|
| `id`       | UUID   | Unique identifier                  |
| `username` | string | Chosen at registration; must be unique |
| `password` | string | Stored hashed (Argon2); never returned in API responses |

**TODO item** (persisted via backend API; scoped to the owning user):

| Field       | Type            | Description                          |
|-------------|-----------------|--------------------------------------|
| `id`        | UUID            | Unique identifier                    |
| `title`     | string          | The task description entered by the user |
| `completed` | boolean         | Whether the task has been checked off |
| `dueDate`   | string \| null  | Optional due date in ISO 8601 format (e.g. `"2026-04-28"`); `null` if not set |
| `userId`    | UUID            | References the owning user           |

---

## 8. Acceptance Criteria (MVP)

- Register, login, and logout flows work end-to-end against the deployed backend
- Add, toggle, and delete all function correctly for the authenticated user
- Empty state is shown when no TODOs are present
- TODOs (including completed state and due date) survive page reload via backend API + PostgreSQL
- A logged-in user cannot see or modify another user's TODOs (verified via integration test)
- All error scenarios in §5.0 surface a clear, user-visible message and do not leak server-side details

---

## 9. Future Considerations (Out of Scope for v1)

- **OAuth2 login** via a third-party provider (e.g., GitHub) as an alternative to username/password — this is the next step after MVP
- Filtering by status (All / Active / Completed)
- Reordering TODOs via drag-and-drop
- Priorities and tags
- Multi-user collaboration on shared lists
- Mobile or desktop native apps
- Cloud sync across devices
