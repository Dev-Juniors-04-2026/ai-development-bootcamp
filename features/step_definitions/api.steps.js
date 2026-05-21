import { Given, When, Then } from '@cucumber/cucumber';
import assert from 'assert';

const API_BASE_URL = process.env.API_BASE_URL || 'http://localhost:8080';

function authHeaders(token, extra = {}) {
  return { ...extra, Authorization: `Bearer ${token}` };
}

When('I create a todo with title {string}', async function (title) {
  this.response = await fetch(`${API_BASE_URL}/api/todos`, {
    method: 'POST',
    headers: authHeaders(this.token, { 'Content-Type': 'application/json' }),
    body: JSON.stringify({ title }),
  });
  this.responseBody = await this.response.json();
});

Then('the response status is {int}', function (expectedStatus) {
  assert.strictEqual(this.response.status, expectedStatus);
});

Then('the response todo has title {string}', function (expectedTitle) {
  assert.strictEqual(this.responseBody.title, expectedTitle);
});

Then('the response todo is not completed', function () {
  assert.strictEqual(this.responseBody.completed, false);
});

Given('a todo with title {string} exists', async function (title) {
  await fetch(`${API_BASE_URL}/api/todos`, {
    method: 'POST',
    headers: authHeaders(this.token, { 'Content-Type': 'application/json' }),
    body: JSON.stringify({ title }),
  });
});

When('I get all todos', async function () {
  this.response = await fetch(`${API_BASE_URL}/api/todos`, {
    headers: authHeaders(this.token),
  });
  this.responseBody = await this.response.json();
});

Then('the response contains {int} todos', function (expectedCount) {
  assert.strictEqual(this.responseBody.length, expectedCount);
});

Then('the response todos include a todo with title {string}', function (expectedTitle) {
  const found = this.responseBody.some((todo) => todo.title === expectedTitle);
  assert.strictEqual(found, true, `No todo with title "${expectedTitle}" found`);
});

When('I toggle the todo', async function () {
  const id = this.todoId ?? this.responseBody.id;
  this.todoId = id;
  this.response = await fetch(`${API_BASE_URL}/api/todos/${id}`, {
    method: 'PATCH',
    headers: authHeaders(this.token),
  });
  this.responseBody = await this.response.json();
});

When('I toggle a todo with id {string}', async function (id) {
  this.response = await fetch(`${API_BASE_URL}/api/todos/${id}`, {
    method: 'PATCH',
    headers: authHeaders(this.token),
  });
  this.responseBody = this.response.status !== 404 ? await this.response.json() : null;
});

When('I delete the todo', async function () {
  const id = this.todoId ?? this.responseBody.id;
  this.todoId = id;
  this.response = await fetch(`${API_BASE_URL}/api/todos/${id}`, {
    method: 'DELETE',
    headers: authHeaders(this.token),
  });
  this.responseBody = this.response.status !== 204 ? await this.response.json() : null;
});

When('I delete a todo with id {string}', async function (id) {
  this.response = await fetch(`${API_BASE_URL}/api/todos/${id}`, {
    method: 'DELETE',
    headers: authHeaders(this.token),
  });
  this.responseBody = this.response.status !== 204 ? await this.response.json() : null;
});

Then('the response todo is completed', function () {
  assert.strictEqual(this.responseBody.completed, true);
});

Then('the todo with title {string} in the list is completed', function (title) {
  const todo = this.responseBody.find((t) => t.title === title);
  assert.ok(todo, `Todo with title "${title}" not found in list`);
  assert.strictEqual(todo.completed, true);
});

Then('the todo with title {string} in the list is not completed', function (title) {
  const todo = this.responseBody.find((t) => t.title === title);
  assert.ok(todo, `Todo with title "${title}" not found in list`);
  assert.strictEqual(todo.completed, false);
});

When('I create a todo with title {string} and due date {string}', async function (title, dueDate) {
  this.response = await fetch(`${API_BASE_URL}/api/todos`, {
    method: 'POST',
    headers: authHeaders(this.token, { 'Content-Type': 'application/json' }),
    body: JSON.stringify({ title, dueDate }),
  });
  this.responseBody = await this.response.json();
});

Then('the response todo has due date {string}', function (expectedDueDate) {
  assert.strictEqual(this.responseBody.dueDate, expectedDueDate);
});

Then('the response todo has no due date', function () {
  assert.strictEqual(this.responseBody.dueDate, null);
});

Then('the todo with title {string} in the list has due date {string}', function (title, expectedDueDate) {
  const todo = this.responseBody.find((t) => t.title === title);
  assert.ok(todo, `Todo with title "${title}" not found in list`);
  assert.strictEqual(todo.dueDate, expectedDueDate);
});
