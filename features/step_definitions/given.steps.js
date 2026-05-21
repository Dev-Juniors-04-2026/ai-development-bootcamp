import { Given } from '@cucumber/cucumber';

Given('I am on the TODO main page', async function () {
  await this.page.goto(this.baseUrl);
  await this.page.locator('#todo-input').waitFor({ state: 'visible' });
});

Given('a TODO with the title {string} exists in the list', async function (title) {
  await this.page.fill('#todo-input', title);
  await this.page.press('#todo-input', 'Enter');
  await this.page.locator('#todo-list li span', { hasText: title }).waitFor({ timeout: 2000 });
});
