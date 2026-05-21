const authView  = document.getElementById('auth-view');
const todoView  = document.getElementById('todo-view');
const authError = document.getElementById('auth-error');

function showAuth() {
  authView.classList.remove('hidden');
  todoView.classList.add('hidden');
}

function showTodo() {
  authView.classList.add('hidden');
  todoView.classList.remove('hidden');
}

function showError(msg) {
  authError.textContent = msg;
  authError.classList.remove('hidden');
}

function clearError() {
  authError.textContent = '';
  authError.classList.add('hidden');
}

function clearSession() {
  localStorage.removeItem('token');
  document.getElementById('username-input').value = '';
  document.getElementById('password-input').value = '';
  document.getElementById('todo-input').value = '';
  document.getElementById('due-date-input').value = '';
  showAuth();
}

// Called by api.js when any request returns 401 — the token expired, was revoked,
// or never was valid. Drop the session and tell the user why.
export function handleSessionExpired() {
  clearSession();
  showError('Your session expired. Please log in again.');
}

export function initAuth({ onAuthenticated }) {
  const token = localStorage.getItem('token');
  if (token) {
    showTodo();
    onAuthenticated(token);
  } else {
    showAuth();
  }

  document.getElementById('login-btn').addEventListener('click', async () => {
    clearError();
    const username = document.getElementById('username-input').value.trim();
    const password = document.getElementById('password-input').value;
    try {
      const res = await fetch('/api/auth/login', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ username, password }),
      });
      if (res.status === 400) {
        const problem = await res.json().catch(() => ({}));
        showError(problem.detail || 'Login failed.');
        return;
      }
      if (!res.ok) { showError('Invalid username or password.'); return; }
      const { token } = await res.json();
      localStorage.setItem('token', token);
      showTodo();
      onAuthenticated(token);
    } catch {
      showError('Could not connect to server.');
    }
  });

  document.getElementById('register-btn').addEventListener('click', async () => {
    clearError();
    const username = document.getElementById('username-input').value.trim();
    const password = document.getElementById('password-input').value;
    try {
      const res = await fetch('/api/auth/register', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ username, password }),
      });
      if (res.status === 409) { showError('Username already taken.'); return; }
      if (res.status === 400) {
        const problem = await res.json().catch(() => ({}));
        showError(problem.detail || 'Registration failed.');
        return;
      }
      if (!res.ok) { showError('Registration failed.'); return; }
      const loginRes = await fetch('/api/auth/login', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ username, password }),
      });
      if (loginRes.status === 400) {
        const problem = await loginRes.json().catch(() => ({}));
        showError(problem.detail || 'Login failed.');
        return;
      }
      if (!loginRes.ok) { showError('Invalid username or password.'); return; }
      const { token } = await loginRes.json();
      localStorage.setItem('token', token);
      showTodo();
      onAuthenticated(token);
    } catch {
      showError('Could not connect to server.');
    }
  });

  document.getElementById('logout-btn').addEventListener('click', () => {
    clearError();
    clearSession();
  });
}
