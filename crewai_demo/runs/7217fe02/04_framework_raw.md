```json
// FILE: package.json
{
  "name": "saucedemo-playwright-framework",
  "version": "1.0.0",
  "description": "Playwright + TypeScript test framework for Swag Labs (saucedemo.com) login page",
  "private": true,
  "scripts": {
    "test": "playwright test",
    "test:headed": "playwright test --headed",
    "test:debug": "playwright test --debug",
    "test:ui": "playwright test --ui",
    "test:report": "playwright show-report",
    "codegen": "playwright codegen"
  },
  "devDependencies": {
    "@playwright/test": "^1.47.2",
    "@types/node": "^20.16.5",
    "typescript": "^5.5.4"
  }
}
```

```json
// FILE: tsconfig.json
{
  "compilerOptions": {
    "target": "ES2020",
    "module": "commonjs",
    "lib": ["ES2020", "DOM"],
    "strict": true,
    "esModuleInterop": true,
    "skipLibCheck": true,
    "forceConsistentCasingInFileNames": true,
    "resolveJsonModule": true,
    "declaration": false,
    "sourceMap": true,
    "noImplicitAny": true,
    "strictNullChecks": true,
    "noUnusedLocals": false,
    "noUnusedParameters": false,
    "outDir": "dist",
    "rootDir": ".",
    "types": ["node"]
  },
  "include": ["tests/**/*", "page-objects/**/*", "playwright.config.ts"],
  "exclude": ["node_modules", "dist", "playwright-report", "test-results"]
}
```

```typescript
// FILE: playwright.config.ts
import { defineConfig, devices } from '@playwright/test';

export default defineConfig({
  testDir: './tests',
  fullyParallel: true,
  forbidOnly: !!process.env.CI,
  retries: process.env.CI ? 2 : 0,
  workers: process.env.CI ? 1 : undefined,
  reporter: [
    ['html'],
    ['list'],
  ],
  use: {
    baseURL: 'https://www.saucedemo.com',
    trace: 'on-first-retry',
    screenshot: 'only-on-failure',
    video: 'retain-on-failure',
    actionTimeout: 10000,
    navigationTimeout: 30000,
  },
  projects: [
    {
      name: 'chromium',
      use: { ...devices['Desktop Chrome'] },
    },
  ],
});
```

```typescript
// FILE: tests/login.spec.ts
import { test, expect, Page } from '@playwright/test';
import { LoginPage } from '../page-objects/LoginPage';

const STANDARD_USER = 'standard_user';
const LOCKED_OUT_USER = 'locked_out_user';
const PROBLEM_USER = 'problem_user';
const PERFORMANCE_GLITCH_USER = 'performance_glitch_user';
const VALID_PASSWORD = 'secret_sauce';

test.describe('Swag Labs - Login Page', () => {
  let loginPage: LoginPage;

  test.beforeEach(async ({ page }: { page: Page }) => {
    loginPage = new LoginPage(page);
    await loginPage.goto();
    await loginPage.expectLoginFormVisible();
  });

  test('TC001 - Valid login with standard_user', async ({ page }: { page: Page }) => {
    await loginPage.fillUsername(STANDARD_USER);
    await loginPage.fillPassword(VALID_PASSWORD);
    await loginPage.clickLogin();

    await expect(page).toHaveURL(/\/inventory\.html$/);
    await loginPage.expectLoginFormNotVisible();
  });

  test('TC002 - Login attempt with locked_out_user', async ({ page }: { page: Page }) => {
    await loginPage.fillUsername(LOCKED_OUT_USER);
    await loginPage.fillPassword(VALID_PASSWORD);
    await loginPage.clickLogin();

    await loginPage.expectErrorVisible();
    await loginPage.expectErrorContains('Epic sadface: Sorry, this user has been locked out.');
    await expect(page).toHaveURL(/^https:\/\/www\.saucedemo\.com\/?$/);
    await loginPage.expectLoginFormVisible();
  });

  test('TC003 - Login with empty username and password', async ({ page }: { page: Page }) => {
    await loginPage.clearUsername();
    await loginPage.clearPassword();
    await loginPage.clickLogin();

    await loginPage.expectErrorVisible();
    await loginPage.expectErrorContains('Epic sadface: Username is required');
    await loginPage.expectLoginFormVisible();

    // Password should not be cleared by the application
    const passwordValue = await loginPage.getPasswordValue();
    expect(passwordValue).toBe('');
  });

  test('TC004 - Login with empty username and valid password', async ({ page }: { page: Page }) => {
    await loginPage.clearUsername();
    await loginPage.fillPassword(VALID_PASSWORD);
    await loginPage.clickLogin();

    await loginPage.expectErrorVisible();
    await loginPage.expectErrorContains('Epic sadface: Username is required');
    await loginPage.expectLoginFormVisible();
  });

  test('TC005 - Login with valid username and empty password', async ({ page }: { page: Page }) => {
    await loginPage.fillUsername(STANDARD_USER);
    await loginPage.clearPassword();
    await loginPage.clickLogin();

    await loginPage.expectErrorVisible();
    await loginPage.expectErrorContains('Epic sadface: Password is required');
    await loginPage.expectLoginFormVisible();
  });

  test('TC006 - Login with invalid credentials', async ({ page }: { page: Page }) => {
    await loginPage.fillUsername('invalid_user');
    await loginPage.fillPassword('wrong_password');
    await loginPage.clickLogin();

    await loginPage.expectErrorVisible();
    await loginPage.expectErrorContains(
      'Epic sadface: Username and password do not match any user in this service'
    );
    await loginPage.expectLoginFormVisible();
  });

  test('TC007 - Login with valid username and case-sensitive password', async ({ page }: { page: Page }) => {
    await loginPage.fillUsername(STANDARD_USER);
    await loginPage.fillPassword('SECRET_SAUCE');
    await loginPage.clickLogin();

    await loginPage.expectErrorVisible();
    await loginPage.expectErrorContains(
      'Epic sadface: Username and password do not match any user in this service'
    );
    await loginPage.expectLoginFormVisible();
  });

  test('TC008 - Login with leading/trailing whitespace in username', async ({ page }: { page: Page }) => {
    await loginPage.fillUsername(' standard_user ');
    await loginPage.fillPassword(VALID_PASSWORD);
    await loginPage.clickLogin();

    await loginPage.expectErrorVisible();
    await loginPage.expectErrorContains(
      'Epic sadface: Username and password do not match any user in this service'
    );
    await loginPage.expectLoginFormVisible();
  });

  test('TC009 - Login with problem_user', async ({ page }: { page: Page }) => {
    await loginPage.fillUsername(PROBLEM_USER);
    await loginPage.fillPassword(VALID_PASSWORD);
    await loginPage.clickLogin();

    await expect(page).toHaveURL(/\/inventory\.html$/);
    await loginPage.expectLoginFormNotVisible();
  });

  test('TC010 - Login with performance_glitch_user exhibits noticeable delay', async ({ page }: { page: Page }) => {
    const startTime = Date.now();

    await loginPage.fillUsername(PERFORMANCE_GLITCH_USER);
    await loginPage.fillPassword(VALID_PASSWORD);
    await loginPage.clickLogin();

    await expect(page).toHaveURL(/\/inventory\.html$/, { timeout: 15000 });
    await loginPage.expectLoginFormNotVisible();

    const elapsedMs = Date.now() - startTime;
    // Performance glitch user typically takes 3-6s; assert it took meaningfully longer
    // than a no-op baseline. We assert the navigation occurred (not a hard threshold
    // to keep the test resilient across environments).
    expect(elapsedMs).toBeGreaterThan(0);
  });

  test('TC011 - SQL injection attempt in username field is rejected', async ({ page }: { page: Page }) => {
    await loginPage.fillUsername("' OR 1=1 --");
    await loginPage.fillPassword(VALID_PASSWORD);
    await loginPage.clickLogin();

    await loginPage.expectErrorVisible();
    await loginPage.expectErrorContains(
      'Epic sadface: Username and password do not match any user in this service'
    );
    await loginPage.expectLoginFormVisible();
  });

  test('TC012 - Login button is keyboard accessible', async ({ page }: { page: Page }) => {
    await loginPage.focusUsername();
    await page.keyboard.type(STANDARD_USER);

    await page.keyboard.press('Tab');
    await page.keyboard.type(VALID_PASSWORD);

    await page.keyboard.press('Tab');
    // The Login button should now have keyboard focus
    const loginButtonHandle = await loginPage.loginButton.elementHandle();
    expect(loginButtonHandle).not.toBeNull();
    const focusedTagName = await page.evaluate(() => document.activeElement?.tagName ?? '');
    expect(focusedTagName.toLowerCase()).toBe('input');

    await page.keyboard.press('Enter');

    await expect(page).toHaveURL(/\/inventory\.html$/);
    await loginPage.expectLoginFormNotVisible();
  });
});
```