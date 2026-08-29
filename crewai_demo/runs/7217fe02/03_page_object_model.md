```typescript
import { Locator, Page, expect } from '@playwright/test';

export class LoginPage {
    readonly page: Page;
    readonly usernameInput: Locator;
    readonly passwordInput: Locator;
    readonly loginButton: Locator;
    readonly errorMessage: Locator;
    readonly errorButton: Locator;
    readonly loginForm: Locator;
    readonly logo: Locator;
    readonly acceptedUsernamesText: Locator;

    constructor(page: Page) {
        this.page = page;

        this.usernameInput = page.locator('input[data-test="username"]');
        this.passwordInput = page.locator('input[data-test="password"]');
        this.loginButton = page.locator('input[data-test="login-button"]');
        this.errorMessage = page.locator('[data-test="error"]');
        this.errorButton = page.locator('.error-button');
        this.loginForm = page.locator('form');
        this.logo = page.locator('.login_logo');
        this.acceptedUsernamesText = page.locator('div.login_credentials');
    }

    async goto(): Promise<void> {
        await this.page.goto('https://www.saucedemo.com');
    }

    async fillUsername(username: string): Promise<void> {
        await this.usernameInput.fill(username);
    }

    async fillPassword(password: string): Promise<void> {
        await this.passwordInput.fill(password);
    }

    async clearUsername(): Promise<void> {
        await this.usernameInput.clear();
    }

    async clearPassword(): Promise<void> {
        await this.passwordInput.clear();
    }

    async clickLogin(): Promise<void> {
        await this.loginButton.click();
    }

    async login(username: string, password: string): Promise<void> {
        await this.fillUsername(username);
        await this.fillPassword(password);
        await this.clickLogin();
    }

    async pressEnterToSubmit(): Promise<void> {
        await this.passwordInput.press('Enter');
    }

    async getErrorMessageText(): Promise<string> {
        return (await this.errorMessage.textContent()) ?? '';
    }

    async getUsernameValue(): Promise<string> {
        return await this.usernameInput.inputValue();
    }

    async getPasswordValue(): Promise<string> {
        return await this.passwordInput.inputValue();
    }

    async dismissError(): Promise<void> {
        await this.errorButton.click();
    }

    async isErrorVisible(): Promise<boolean> {
        return await this.errorMessage.isVisible();
    }

    async isLoginButtonVisible(): Promise<boolean> {
        return await this.loginButton.isVisible();
    }

    async isUsernameVisible(): Promise<boolean> {
        return await this.usernameInput.isVisible();
    }

    async isPasswordVisible(): Promise<boolean> {
        return await this.passwordInput.isVisible();
    }

    async isLoginFormVisible(): Promise<boolean> {
        return await this.loginForm.isVisible();
    }

    async expectErrorContains(text: string): Promise<void> {
        await expect(this.errorMessage).toContainText(text);
    }

    async expectErrorVisible(): Promise<void> {
        await expect(this.errorMessage).toBeVisible();
    }

    async expectErrorNotVisible(): Promise<void> {
        await expect(this.errorMessage).not.toBeVisible();
    }

    async expectLoginFormVisible(): Promise<void> {
        await expect(this.loginForm).toBeVisible();
    }

    async expectLoginFormNotVisible(): Promise<void> {
        await expect(this.loginForm).not.toBeVisible();
    }

    async expectUsernamePlaceholder(placeholder: string): Promise<void> {
        await expect(this.usernameInput).toHaveAttribute('placeholder', placeholder);
    }

    async expectPasswordPlaceholder(placeholder: string): Promise<void> {
        await expect(this.passwordInput).toHaveAttribute('placeholder', placeholder);
    }

    async focusUsername(): Promise<void> {
        await this.usernameInput.focus();
    }

    async focusPassword(): Promise<void> {
        await this.passwordInput.focus();
    }

    async focusLoginButton(): Promise<void> {
        await this.loginButton.focus();
    }
}
```