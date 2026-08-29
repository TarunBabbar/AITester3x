I'll create a complete, runnable Playwright + TypeScript test framework for the Swag Labs login page. Let me design each file with Playwright best practices in mind.

```typescript
// FILE: package.json
{
  "name": "swag-labs-playwright-framework",
  "version": "1.0.0",
  "description": "Playwright + TypeScript test framework for Swag Labs login",
  "main": "index.js",
  "scripts": {
    "test": "playwright test",
    "test:headed": "playwright test --headed",
    "test:debug": "playwright test --debug",
    "test:report": "playwright show-report",
    "codegen": "playwright codegen http://saucedemo.com"
  },
  "keywords": [
    "playwright",
    "typescript",
    "testing",
    "e2e",
    "saucelabs"
  ],
  "author": "",
  "license": "MIT",
  "devDependencies": {
    "@playwright/test": "^1.45.0",
    "@types/node": "^20.14.0",
    "typescript": "^5.5.0"
  }
}
```

```typescript
// FILE: tsconfig.json
{
  "compilerOptions": {
    "target": "ES2022",
    "module": "commonjs",
    "lib": ["ES2022", "DOM"],
    "moduleResolution": "node",
    "esModuleInterop": true,
    "forceConsistentCasingInFileNames": true,
    "strict": true,
    "skipLibCheck": true,
    "resolveJsonModule": true,
    "outDir": "./dist",
    "rootDir": "./",
    "baseUrl": "./",
    "paths": {
      "@page-objects/*": ["page-objects/*"],
      "@tests/*": ["tests/*"]
    }
  },
  "include": [
    "tests/**/*.ts",
    "page-objects/**/*.ts",
    "playwright.config.ts"
  ],
  "exclude": [
    "node_modules",
    "dist",
    "test-results",
    "playwright-report"
  ]
}
```

```typescript
// FILE: playwright.config.ts
import { defineConfig, devices } from '@playwright/test';

/**
 * Playwright configuration for Swag Labs login tests
 * @see https://playwright.dev/docs/test-configuration
 */
export default defineConfig({
  testDir: './tests',
  fullyParallel: true,
  forbidOnly: !!process.env.CI,
  retries: process.env.CI ? 2 : 0,
  workers: process.env.CI ? 1 : undefined,
  reporter: [
    ['html', { open: 'never' }],
    ['list'],
    ['json', { outputFile: 'test-results/results.json' }],
  ],
  use: {
    baseURL: 'http://saucedemo.com',
    trace: 'on-first-retry',
    screenshot: 'only-on-failure',
    video: 'retain-on-failure',
    actionTimeout: 10_000,
    navigationTimeout: 30_000,
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
// FILE: tests/swag-labs-login.spec.ts
import { test, expect, Page } from '@playwright/test';
import { SwagLabsLoginPage } from '../page-objects/SwagLabsLoginPage';

/**
 * Test suite for Swag Labs Login functionality
 * Covers P0 login scenarios including successful login, locked out user, and invalid credentials
 */
test.describe('Swag Labs Login - P0 Scenarios', () => {
  let loginPage: SwagLabsLoginPage;

  test.beforeEach(async ({ page }: { page: Page) => {
    loginPage = new SwagLabsLoginPage(page);
    await loginPage.navigate();
    // Verify the login page is loaded before each test
    await expect(page).toHaveTitle('Swag Labs');
    await expect(loginPage.loginButton).toBeVisible();
  });

  test('TC001: Successful login with valid standard_user credentials', async ({ page }) => {
    // Arrange
    const username = 'standard_user';
    const password = 'secret_sauce';

    // Act
    await loginPage.fillUsername(username);
    await loginPage.fillPassword(password);
    await loginPage.clickLogin();

    // Assert - User should be navigated to the inventory page
    await expect(page).toHaveURL(/.*\/inventory\.html$/);
    // The login button should no longer be visible after successful login
    await expect(loginPage.loginButton).not.toBeVisible();
  });

  test('TC002: Login fails with locked_out_user account', async ({ page }) => {
    // Arrange
    const username = 'locked_out_user';
    const password = 'secret_sauce';

    // Act
    await loginPage.fillUsername(username);
    await loginPage.fillPassword(password);
    await loginPage.clickLogin();

    // Assert - Login should be denied and an error should be displayed
    await expect(loginPage.errorMessage).toBeVisible();
    const errorText = await loginPage.getErrorMessage();
    expect(errorText.toLowerCase()).toContain('locked out');
    // User should remain on the login page
    await expect(page).toHaveURL('http://saucedemo.com/');
  });

  test('TC003: Login fails with invalid password', async ({ page }) => {
    // Arrange
    const username = 'standard_user';
    const password = 'wrong_password';

    // Act
    await loginPage.fillUsername(username);
    await loginPage.fillPassword(password);
    await loginPage.clickLogin();

    // Assert - Login should be denied and a mismatch error should be displayed
    await expect(loginPage.errorMessage).toBeVisible();
    const errorText = await loginPage.getErrorMessage();
    expect(errorText.toLowerCase()).toContain('do not match');
    // User should remain on the login page
    await expect(page).toHaveURL('http://saucedemo.com/');
  });
});
```

**Setup & run instructions:**

1. Place the POM in `page-objects/SwagLabsLoginPage.ts` (matching the import path used in the spec).
2. Save the four files above at the indicated paths.
3. Install dependencies and browsers:
   ```bash
   npm install
   npx playwright install chromium
   ```
4. Run the tests:
   ```bash
   npx playwright test
   ```

**Design highlights:**
- **`playwright.config.ts`**: Single Chromium project, `baseURL` set to `http://saucedemo.com`, auto-screenshots/videos on failure, trace on retry, and CI-aware retries/workers.
- **`tsconfig.json`**: Strict mode, ES2022 target, `commonjs` module to match `@playwright/test`'s runtime, plus path aliases for clean imports.
- **`swag-labs-login.spec.ts`**: One `test()` block per TC, isolated `beforeEach` that re-navigates to the login page so tests are independent and order-independent. Assertions verify both the URL transition (for success) and the error message content (for failures) per the expected criteria.