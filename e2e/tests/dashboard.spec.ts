import { test, expect } from '@playwright/test';

test.describe('Dashboard Acceptance Tests', () => {

    test.beforeEach(async ({ page }) => {
        await page.goto('/');

        // Check if we hit login page via URL or form presence
        try {
            // The app might redirect to /login
            if (page.url().includes('login') || await page.locator('input[name="username"]').isVisible()) {
                await page.fill('input[name="username"]', 'admin');
                await page.fill('input[name="password"]', 'default123');
                await page.getByRole('button', { name: /Sign in/i }).click();

                // Wait for login to complete and navigate to dashboard
                await page.waitForURL('**/dashboard', { timeout: 10000 });
            }
        } catch (e) {
            // Ignore if no login needed
        }
    });

    test('should load the dashboard and display primary widgets', async ({ page }) => {
        await page.waitForURL('**/dashboard', { timeout: 10000 });
        await expect(page).toHaveURL(/.*dashboard/);

        // Verify core UI elements exist
        await expect(page.locator('text=Personal Dashboard').first().or(page.locator('text=Portfolio').first())).toBeVisible({ timeout: 10000 });

        // At least some content cards should be rendered
        const cards = page.locator('.bg-card, .bg-white');
        expect(await cards.count()).toBeGreaterThan(0);
    });

});
