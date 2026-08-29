```typescript
import { Page, Locator } from '@playwright/test';

export class SwagLabsLoginPage {
  private readonly page: Page;

  // Locators
  readonly usernameInput: Locator;
  readonly passwordInput: Locator;
  readonly loginButton: Locator;
  readonly errorMessage: Locator;
  readonly loginLogo: Locator;
  readonly acceptedUsernamesText: Locator;

  constructor(page: Page) {
    this.page = page;

    // Element locators
    this.usernameInput = page.locator('#user-name');
    this.passwordInput = page.locator('#password');
    this.loginButton = page.locator('#login-button');
    this.errorMessage = page.locator('[data-test="error"]');
    this.loginLogo = page.locator('.login_logo');
    this.acceptedUsernamesText = page.locator('.login_credentials');
  }

  // Action methods
  async navigate(): Promise<void> {
    await this.page.goto('http://saucedemo.com');
  }

  async fillUsername(username: string): Promise<void> {
    await this.usernameInput.fill(username);
  }

  async fillPassword(password: string): Promise<void> {
    await this.passwordInput.fill(password);
  }

  async clickLogin(): Promise<void> {
    await this.loginButton.click();
  }

  async login(username: string, password: string): Promise<void> {
    await this.fillUsername(username);
    await this.fillPassword(password);
    await this.clickLogin();
  }

  async getErrorMessage(): Promise<string> {
    return (await this.errorMessage.textContent()) ?? '';
  }

  async isErrorVisible(): Promise<boolean> {
    return await this.errorMessage.isVisible();
  }

  async getLoginLogoText(): Promise<string> {
    return (await this.loginLogo.textContent()) ?? '';
  }

  async getAcceptedUsernamesText(): Promise<string> {
    return (await this.acceptedUsernamesText.textContent()) ?? '';
  }
}
```