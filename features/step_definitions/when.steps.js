import { When } from '@cucumber/cucumber';

When('I add a todo titled {string} via {string}', async function (title, method) {
  await this.page.fill('#todo-input', title);
  if (method === 'Add button') {
    await this.page.click('#add-btn');
  } else {
    await this.page.press('#todo-input', 'Enter');
  }
});

When('I add a todo titled {string}', async function (title) {
  await this.page.fill('#todo-input', title);
  await this.page.press('#todo-input', 'Enter');
});

When('I add a todo titled {string} with due date {string}', async function (title, date) {
  await this.page.fill('#todo-input', title);
  await this.page.fill('#due-date-input', date);
  await this.page.press('#todo-input', 'Enter');
});

When('I delete the todo {string}', async function (title) {
  await this.page
    .locator('#todo-list li')
    .filter({ has: this.page.locator('span', { hasText: title }) })
    .locator('.delete-btn')
    .click();
});

When('I toggle the todo {string}', async function (title) {
  await this.page
    .locator('#todo-list li')
    .filter({ hasText: title })
    .locator('input[type="checkbox"]')
    .click();
});

When('I reload the page', async function () {
  await this.page.reload();
  await this.page.locator('#todo-input').waitFor({ state: 'visible' });
});
