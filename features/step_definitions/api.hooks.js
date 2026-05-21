import { BeforeAll, AfterAll, Before } from '@cucumber/cucumber';
import { spawn } from 'child_process';
import { resolve } from 'path';
import { fileURLToPath } from 'url';

const __dirname = fileURLToPath(new URL('.', import.meta.url));
const backendDir = resolve(__dirname, '../../backend');

let backendProcess;

BeforeAll({ timeout: 120_000 }, async function () {
  backendProcess = spawn('mvn', ['spring-boot:run'], {
    cwd: backendDir,
    stdio: 'inherit',
    env: { ...process.env, SPRING_PROFILES_ACTIVE: 'test' },
  });

  const deadline = Date.now() + 90_000;
  while (Date.now() < deadline) {
    try {
      await fetch('http://localhost:8080/api/todos');
      return;
    } catch {
      await new Promise((r) => setTimeout(r, 1000));
    }
  }
  throw new Error('Backend did not start within 90 seconds');
});

AfterAll(async function () {
  await fetch('http://localhost:8080/api/todos/reset', { method: 'DELETE' }).catch(() => {});
  backendProcess?.kill();
});

const API_TEST_USER = { username: 'api-bdd-user', password: 'api-bdd-password' };

Before({ tags: '@api' }, async function () {
  await fetch('http://localhost:8080/api/todos/reset', { method: 'DELETE' });
  await fetch('http://localhost:8080/api/auth/register', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(API_TEST_USER),
  });
  const res = await fetch('http://localhost:8080/api/auth/login', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(API_TEST_USER),
  });
  const { token } = await res.json();
  this.token = token;
});
