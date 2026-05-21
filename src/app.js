import { fetchTodos, createTodo, toggleTodo, deleteTodo, setToken, setOnUnauthorized } from './api.js';
import { initAuth, handleSessionExpired } from './auth.js';

setOnUnauthorized(handleSessionExpired);

const input        = document.getElementById('todo-input');
const dueDateInput = document.getElementById('due-date-input');
const addBtn       = document.getElementById('add-btn');
const list         = document.getElementById('todo-list');
const emptyState   = document.getElementById('empty-state');

function render(todos) {
  list.innerHTML = '';
  todos.forEach(todo => {
    const li = document.createElement('li');
    li.dataset.id = todo.id;
    if (todo.completed) li.classList.add('completed');

    const checkbox = document.createElement('input');
    checkbox.type = 'checkbox';
    checkbox.checked = todo.completed;

    const span = document.createElement('span');
    span.textContent = todo.title;

    const btn = document.createElement('button');
    btn.className = 'delete-btn';
    btn.textContent = '✕';

    li.append(checkbox, span);

    if (todo.dueDate) {
      const due = document.createElement('span');
      due.className = 'due-date';
      due.dataset.dueDate = todo.dueDate;
      due.textContent = todo.dueDate;
      li.appendChild(due);
    }

    li.appendChild(btn);
    list.appendChild(li);
  });
  emptyState.classList.toggle('hidden', todos.length > 0);
}

async function refresh() {
  const todos = await fetchTodos();
  render(todos);
}

async function handleAdd() {
  const title = input.value.trim();
  if (!title) return;
  const dueDate = dueDateInput.value || null;
  input.value = '';
  dueDateInput.value = '';
  await createTodo(title, dueDate);
  await refresh();
}

addBtn.addEventListener('click', handleAdd);
input.addEventListener('keydown', e => { if (e.key === 'Enter') handleAdd(); });

list.addEventListener('change', async e => {
  if (e.target.matches('input[type="checkbox"]')) {
    const id = e.target.closest('li').dataset.id;
    await toggleTodo(id);
    await refresh();
  }
});

list.addEventListener('click', async e => {
  if (e.target.matches('.delete-btn')) {
    const id = e.target.closest('li').dataset.id;
    await deleteTodo(id);
    await refresh();
  }
});

initAuth({
  async onAuthenticated(token) {
    setToken(token);
    try {
      await refresh();
    } catch {
      // 401s flow through setOnUnauthorized → handleSessionExpired; swallow the rest so the promise doesn't reject unhandled.
    }
  },
});
