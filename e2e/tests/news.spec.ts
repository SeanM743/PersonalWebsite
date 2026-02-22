import { test, expect } from '@playwright/test';

test.describe('News Page Acceptance Tests', () => {
    test.beforeEach(async ({ page }) => {
        await page.goto('/news');

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

            // Navigate back to the news page after successful login
            await page.goto('/news');

        } catch (e) {
            // Login form didn't appear, assume we are already authenticated or don't need to be
            if (!page.url().includes('news')) {
                await page.goto('/news');
            }
        }
    });

    test('should load news tabs and handle the entertainment tab without crashing', async ({ page }) => {
        await page.waitForURL('**/news', { timeout: 10000 });
        await expect(page).toHaveURL(/.*news/);

        // Verify the Daily Briefing header exists (wait up to 20s if API is slow)
        await expect(page.getByText('Daily Briefing', { exact: false }).first()).toBeVisible({ timeout: 20000 });

        // Verify all static tabs exist
        const tabs = ['Financial', 'Sports', 'Politics', 'Entertainment', 'Science', 'Misc'];
        for (const tab of tabs) {
            await expect(page.getByRole('button', { name: tab })).toBeVisible();
        }

        // Click the Entertainment tab
        const entertainmentTab = page.getByRole('button', { name: 'Entertainment' });
        await entertainmentTab.click();

        await page.waitForTimeout(2000);

        // Validate we can see either empty state, articles, or 'no news' fallback
        const emptyState = page.locator('text=Empty Section');
        const articles = page.locator('article');
        const noNews = page.locator('text=We couldn\'t find fresh articles');

        await expect(emptyState.or(articles.first()).or(noNews)).toBeVisible({ timeout: 15000 });
    });
});
