import { Before, After, BeforeAll, AfterAll, setDefaultTimeout } from '@cucumber/cucumber';
import { chromium } from 'playwright';
import { createServer, request as httpRequest } from 'http';
import { readFileSync } from 'fs';
import { resolve, extname } from 'path';
import { fileURLToPath } from 'url';

const __dirname = fileURLToPath(new URL('.', import.meta.url));
const projectRoot = resolve(__dirname, '../../');

setDefaultTimeout(3_000);

const MIME = {
  '.html': 'text/html',
  '.js':   'application/javascript',
  '.css':  'text/css',
};

function startServer() {
  return new Promise((res) => {
    const server = createServer((req, reply) => {
      // The test server now proxies any /api/* request to localhost:8080, so
      // BASE = '' works correctly in both environments:
      if (req.url.startsWith('/api/')) {
        const pr = httpRequest(
          { hostname: 'localhost', port: 8080, path: req.url, method: req.method, headers: req.headers },
          (backendRes) => {
            reply.writeHead(backendRes.statusCode, backendRes.headers);
            backendRes.pipe(reply);
          }
        );
        pr.on('error', () => { reply.writeHead(502); reply.end('Bad gateway'); });
        req.pipe(pr);
        return;
      }
      const filePath = projectRoot + (req.url === '/' ? '/index.html' : req.url);
      try {
        const body = readFileSync(filePath);
        reply.writeHead(200, { 'Content-Type': MIME[extname(filePath)] ?? 'text/plain' });
        reply.end(body);
      } catch {
        reply.writeHead(404);
        reply.end('Not found');
      }
    });
    server.listen(0, '127.0.0.1', () => res(server));
  });
}

let server;
let baseUrl;

BeforeAll(async function () {
  server = await startServer();
  baseUrl = `http://127.0.0.1:${server.address().port}`;
});

AfterAll(async function () {
  await fetch('http://localhost:8080/api/todos/reset', { method: 'DELETE' }).catch(() => {});
  await new Promise((res) => server?.close(res));
});

const TEST_USER = { username: 'bdd-user', password: 'bdd-password' };

async function registerAndLogin({ username, password }) {
  await fetch('http://localhost:8080/api/auth/register', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ username, password }),
  });
  const res = await fetch('http://localhost:8080/api/auth/login', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ username, password }),
  });
  const { token } = await res.json();
  return token;
}

Before({ tags: 'not @api' }, async function () {
  await fetch('http://localhost:8080/api/todos/reset', { method: 'DELETE' }).catch(() => {});
  this.baseUrl = baseUrl;
  this.browser = await chromium.launch();
  this.page = await this.browser.newPage();
});

Before({ tags: 'not @api and not @auth' }, async function () {
  const token = await registerAndLogin(TEST_USER);
  await this.page.addInitScript((t) => localStorage.setItem('token', t), token);
});

After(async function () {
  await this.browser?.close();
});
