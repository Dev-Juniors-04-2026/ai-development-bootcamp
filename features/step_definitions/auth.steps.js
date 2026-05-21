import { Given, When, Then } from '@cucumber/cucumber';
import assert from 'assert';

const API_BASE = process.env.API_BASE_URL || 'http://localhost:8080';

async function registerUser(username, password) {
  await fetch(`${API_BASE}/api/auth/register`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ username, password }),
  });
}

async function loginUser(username, password) {
  const res = await fetch(`${API_BASE}/api/auth/login`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ username, password }),
  });
  return res.json().then(d => d.token);
}

Given('I am on the login page', async function () {
  await this.page.goto(this.baseUrl);
  await this.page.locator('#auth-view').waitFor({ state: 'visible' });
});

Given('a user with username {string} is already registered', async function (username) {
  await registerUser(username, 'defaultpassword');
});

Given('a user with username {string} and password {string} is already registered', async function (username, password) {
  await registerUser(username, password);
});

Given('I am logged in as username {string} with password {string}', async function (username, password) {
  await registerUser(username, password);
  const token = await loginUser(username, password);
  await this.page.evaluate(t => localStorage.setItem('token', t), token);
  await this.page.goto(this.baseUrl);
  await this.page.waitForSelector('#todo-view:not(.hidden)');
});

When('I register with username {string} and password {string}', async function (username, password) {
  await this.page.fill('#username-input', username);
  await this.page.fill('#password-input', password);
  await this.page.click('#register-btn');
});

When('I log in with username {string} and password {string}', async function (username, password) {
  await this.page.fill('#username-input', username);
  await this.page.fill('#password-input', password);
  await this.page.click('#login-btn');
});

When('I click log out', async function () {
  await this.page.click('#logout-btn');
});

When('the stored token is invalid and the app reloads', async function () {
  // Plant a token the backend's JwtFilter will reject (malformed signature).
  // From the frontend's POV this is indistinguishable from an expired token —
  // both produce a 401, both must route the user back to login. The following
  // Then steps wait for the resulting UI state (auth view + error), which is
  // only reachable via the 401 path.
  await this.page.evaluate(() => localStorage.setItem('token', 'not.a.real.jwt'));
  await this.page.goto(this.baseUrl);
});

When('I type {int} characters into the username field', async function (count) {
  await this.page.fill('#username-input', 'a'.repeat(count));
});

When('I type {int} characters into the password field', async function (count) {
  await this.page.fill('#password-input', 'a'.repeat(count));
});

Then('the login form is visible', async function () {
  await this.page.locator('#auth-view').waitFor({ state: 'visible' });
});

Then('the login form is not visible', async function () {
  await this.page.locator('#auth-view').waitFor({ state: 'hidden' });
});

Then('the todo list is visible', async function () {
  await this.page.locator('#todo-view').waitFor({ state: 'visible' });
});

Then('the todo list is not visible', async function () {
  await this.page.locator('#todo-view').waitFor({ state: 'hidden' });
});

Then('I see the auth error {string}', async function (expectedMessage) {
  await this.page.locator('#auth-error', { hasText: expectedMessage }).waitFor({ state: 'visible' });
});

Then('the username field contains at most {int} characters', async function (maxLength) {
  const value = await this.page.locator('#username-input').inputValue();
  assert.ok(value.length <= maxLength, `Expected at most ${maxLength} chars but got ${value.length}`);
});

Then('the password field contains at most {int} characters', async function (maxLength) {
  const value = await this.page.locator('#password-input').inputValue();
  assert.ok(value.length <= maxLength, `Expected at most ${maxLength} chars but got ${value.length}`);
});
