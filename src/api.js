const BASE = '';

let _token = null;
let _onUnauthorized = null;

export function setToken(token) {
  _token = token;
}

export function setOnUnauthorized(handler) {
  _onUnauthorized = handler;
}

function authHeaders() {
  return {
    'Content-Type': 'application/json',
    ...(_token ? { 'Authorization': `Bearer ${_token}` } : {}),
  };
}

// Centralizes 401 handling: token is invalid (expired, revoked, tampered), so we
// drop it and let the auth module return the user to the login screen.
function ensureOk(res, label) {
  if (res.status === 401) {
    _token = null;
    if (_onUnauthorized) _onUnauthorized();
    throw new Error(`${label} unauthorized`);
  }
  if (!res.ok) throw new Error(`${label} failed: ${res.status}`);
  return res;
}

export async function fetchTodos() {
  const res = await fetch(`${BASE}/api/todos`, { headers: authHeaders() });
  ensureOk(res, 'GET /api/todos');
  return res.json();
}

export async function createTodo(title, dueDate = null) {
  const body = { title };
  if (dueDate) body.dueDate = dueDate;
  const res = await fetch(`${BASE}/api/todos`, {
    method: 'POST',
    headers: authHeaders(),
    body: JSON.stringify(body),
  });
  ensureOk(res, 'POST /api/todos');
  return res.json();
}

export async function toggleTodo(id) {
  const res = await fetch(`${BASE}/api/todos/${id}`, { method: 'PATCH', headers: authHeaders() });
  ensureOk(res, `PATCH /api/todos/${id}`);
  return res.json();
}

export async function deleteTodo(id) {
  const res = await fetch(`${BASE}/api/todos/${id}`, { method: 'DELETE', headers: authHeaders() });
  ensureOk(res, `DELETE /api/todos/${id}`);
}
