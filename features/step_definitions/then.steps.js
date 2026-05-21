import { Then } from '@cucumber/cucumber';
import assert from 'assert';

Then('a TODO with the title {string} appears in the list', async function (expectedTitle) {
  await this.page.waitForFunction(
    (title) => [...document.querySelectorAll('#todo-list li span')].some(s => s.textContent === title),
    expectedTitle,
    { timeout: 2000 }
  );
});

Then('{string} is no longer in the list', async function (title) {
  await this.page.locator('#todo-list li span', { hasText: title }).waitFor({ state: 'detached', timeout: 2000 });
});

Then('{int} TODOs appear in the list', async function (expectedCount) {
  await this.page.waitForFunction(
    (count) => document.querySelectorAll('#todo-list li').length === count,
    expectedCount,
    { timeout: 2000 }
  );
});

Then('the text field is empty', async function () {
  await this.page.waitForFunction(
    () => document.querySelector('#todo-input').value === '',
    null,
    { timeout: 2000 }
  );
});

Then('the empty state is visible', async function () {
  await this.page.locator('#empty-state').waitFor({ state: 'visible', timeout: 2000 });
});

Then('the empty state is not visible', async function () {
  await this.page.locator('#empty-state').waitFor({ state: 'hidden', timeout: 2000 });
});

Then('{string} appears as the first TODO in the list', async function (title) {
  await this.page
    .locator('#todo-list li')
    .first()
    .locator('span', { hasText: title })
    .waitFor({ state: 'visible', timeout: 2000 });
});

Then('{string} appears as the last TODO in the list', async function (title) {
  await this.page
    .locator('#todo-list li')
    .last()
    .locator('span', { hasText: title })
    .waitFor({ state: 'visible', timeout: 2000 });
});

Then('the TODO {string} shows the due date {string}', async function (title, expectedDate) {
  const row = this.page
    .locator('#todo-list li')
    .filter({ has: this.page.locator('span', { hasText: title }) })
    .first();
  await row.waitFor({ state: 'visible', timeout: 2000 });
  await row.locator('.due-date', { hasText: expectedDate }).waitFor({ state: 'visible', timeout: 2000 });
});

Then('the TODO {string} shows no due date', async function (title) {
  const row = this.page
    .locator('#todo-list li')
    .filter({ has: this.page.locator('span', { hasText: title }) })
    .first();
  await row.waitFor({ state: 'visible', timeout: 2000 });
  const count = await row.locator('.due-date').count();
  assert.strictEqual(count, 0);
});

Then('the TODO {string} appears with strikethrough', async function (title) {
  await this.page
    .locator('#todo-list li')
    .filter({ hasText: title })
    .and(this.page.locator('.completed'))
    .locator('span')
    .waitFor({ state: 'visible', timeout: 2000 });
});

Then('the TODO {string} does not appear with strikethrough', async function (title) {
  await this.page
    .locator('#todo-list li')
    .filter({ hasText: title })
    .and(this.page.locator(':not(.completed)'))
    .locator('span')
    .waitFor({ state: 'visible', timeout: 2000 });
});
