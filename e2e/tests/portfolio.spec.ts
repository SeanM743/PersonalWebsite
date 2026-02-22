import { test, expect } from '@playwright/test';

test.describe('Portfolio/Financial Page Acceptance Tests', () => {

    test.beforeEach(async ({ page }) => {
        await page.goto('/portfolio');

        try {
            // Wait for login form up to 3 seconds implicitly
            await page.waitForSelector('input[name="username"]', { timeout: 3000 });
            await page.fill('input[name="username"]', 'admin');
            await page.fill('input[name="password"]', 'default123');
            await page.getByRole('button', { name: /Sign in/i }).click();

            // Wait for the login form to disappear
            await page.waitForSelector('input[name="username"]', { state: 'hidden', timeout: 5000 });

            // The login component forces a redirect to /dashboard
            await page.waitForURL('**/dashboard', { timeout: 5000 });

            // Navigate back to the portfolio page after successful login
            await page.goto('/portfolio');

        } catch (e) {
            // Login form didn't appear, assume we are already authenticated or don't need to be
            if (!page.url().includes('portfolio')) {
                await page.goto('/portfolio');
            }
        }
    });

    test('should load portfolio overview and allow clicking through accounts', async ({ page }) => {
        await page.waitForURL('**/portfolio', { timeout: 10000 });
        await expect(page).toHaveURL(/.*portfolio/);

        // Verify the Net Worth and Overview header exists
        await expect(page.getByText('Net Worth').first()).toBeVisible({ timeout: 10000 });
        await expect(page.getByText('All Accounts', { exact: false }).first()).toBeVisible();

        // Check if there are accounts in the sidebar (we should have at least the generic overview button)
        const overviewButton = page.locator('button').filter({ hasText: 'Overview' });
        await expect(overviewButton).toBeVisible();

        // Look for default accounts, specifically "Stock Portfolio" or "Fidelity Cash"
        // Wait for accounts to load (sidebar loading state disappearing)
        await page.waitForSelector('text=Loading...', { state: 'hidden', timeout: 5000 }).catch(() => { });

        // Try to click on the Stock Portfolio account
        const stockPortfolioBtn = page.locator('button').filter({ hasText: 'Stock Portfolio' });

        // If the account exists, click it and verify the details pane renders
        if (await stockPortfolioBtn.isVisible()) {
            await stockPortfolioBtn.click();

            // Verify Account Details header loaded with "Stock Portfolio"
            await expect(page.locator('h1').filter({ hasText: 'Stock Portfolio' })).toBeVisible({ timeout: 5000 });

            // Verify tabs appear for stock account
            await expect(page.getByRole('button', { name: 'Holdings' })).toBeVisible();
            await expect(page.getByRole('button', { name: 'Transactions' })).toBeVisible();
            await expect(page.getByRole('button', { name: 'Performance' })).toBeVisible();

            // Click transactions tab and verify its rendering
            await page.getByRole('button', { name: 'Transactions' }).click();
            await expect(page.locator('h3').filter({ hasText: 'Transaction History' })).toBeVisible({ timeout: 5000 });

        } else {
            // Fallback: click the first account in the list after "Overview"
            // The overview button is the first one, the rest are user accounts.
            const allButtons = page.locator('.w-64 button'); // sidebar buttons
            if (await allButtons.count() > 1) {
                await allButtons.nth(1).click();

                // Verify Account Details sections
                await expect(page.locator('text=Current Balance').first()).toBeVisible({ timeout: 5000 });
            }
        }
    });
});
