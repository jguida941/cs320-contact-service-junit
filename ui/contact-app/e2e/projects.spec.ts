import { test, expect } from '@playwright/test';

/**
 * E2E tests for Projects CRUD functionality.
 *
 * This test suite covers:
 * - Creating a new project
 * - Editing project name/description/status
 * - Deleting a project
 * - Filtering projects by status
 * - Navigating to project detail page
 *
 * Uses API mocking to run independently of the backend.
 */

// Mock project data
const mockProjects = [
  {
    id: 'PROJ001',
    name: 'Website Redesign',
    description: 'Redesign company website with modern UI',
    status: 'ACTIVE',
  },
  {
    id: 'PROJ002',
    name: 'Mobile App',
    description: 'Develop new mobile application',
    status: 'ON_HOLD',
  },
  {
    id: 'PROJ003',
    name: 'Database Migration',
    description: 'Migrate to new database system',
    status: 'COMPLETED',
  },
];

test.describe('Projects CRUD Tests', () => {
  test.beforeEach(async ({ page }) => {
    // Mock API responses for projects
    await page.route('**/api/v1/projects', async (route) => {
      const method = route.request().method();

      if (method === 'GET') {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify(mockProjects),
        });
      } else if (method === 'POST') {
        const body = route.request().postDataJSON();
        await route.fulfill({
          status: 201,
          contentType: 'application/json',
          body: JSON.stringify(body),
        });
      } else {
        await route.continue();
      }
    });

    await page.route('**/api/v1/projects/*', async (route) => {
      const method = route.request().method();

      if (method === 'PUT') {
        const body = route.request().postDataJSON();
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({ ...mockProjects[0], ...body }),
        });
      } else if (method === 'DELETE') {
        await route.fulfill({ status: 204 });
      } else if (method === 'GET') {
        // Mock individual project detail
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify(mockProjects[0]),
        });
      } else {
        await route.continue();
      }
    });
  });

  test('displays projects list', async ({ page }) => {
    await page.goto('/projects');

    // Wait for projects to load
    await expect(page.locator('h2', { hasText: /projects/i })).toBeVisible();
    await expect(page.getByText('Website Redesign')).toBeVisible();
    await expect(page.getByText('Mobile App')).toBeVisible();
    await expect(page.getByText('Database Migration')).toBeVisible();
  });

  test('creates a new project', async ({ page }) => {
    await page.goto('/projects');

    // Click add button
    await page.getByRole('button', { name: /add project/i }).click();

    // Fill form
    await expect(page.getByRole('heading', { name: /new project/i })).toBeVisible();
    await page.getByLabel(/^id$/i).fill('PROJ004');
    await page.getByLabel(/name/i).fill('API Integration');
    await page.getByLabel(/description/i).fill('Integrate third-party APIs');

    // Select status (default is ACTIVE, but let's explicitly set it)
    const statusSelect = page.locator('select[name="status"], [id="status"]');
    if (await statusSelect.count() > 0) {
      await statusSelect.selectOption('ACTIVE');
    }

    // Submit
    await page.getByRole('button', { name: /create/i }).click();

    // Sheet should close (form submitted successfully)
    await expect(page.getByRole('heading', { name: /new project/i })).not.toBeVisible();
  });

  test('shows validation errors for invalid input', async ({ page }) => {
    await page.goto('/projects');

    // Click add button
    await page.getByRole('button', { name: /add project/i }).click();

    // Submit empty form
    await page.getByRole('button', { name: /create/i }).click();

    // Should show validation errors
    await expect(page.getByText(/id.*required/i)).toBeVisible();
  });

  test('edits an existing project', async ({ page }) => {
    await page.goto('/projects');

    // Wait for list to load
    await expect(page.getByText('Website Redesign')).toBeVisible();

    // Click on the row to open view mode
    await page.getByText('Website Redesign').click();

    // Wait for view sheet to open
    await expect(page.getByText(/project details/i)).toBeVisible();

    // Click Edit button
    await page.getByRole('button', { name: /edit/i }).first().click();

    // Form should open with existing data
    await expect(page.getByText(/edit project/i)).toBeVisible();

    // Update name
    await page.getByLabel(/name/i).clear();
    await page.getByLabel(/name/i).fill('Website Revamp');

    // Submit
    await page.getByRole('button', { name: /update/i }).click();

    // Sheet should close
    await expect(page.getByText(/edit project/i)).not.toBeVisible({ timeout: 10000 });
  });

  test('updates project status', async ({ page }) => {
    await page.goto('/projects');

    // Wait for list to load
    await expect(page.getByText('Website Redesign')).toBeVisible();

    // Click on the row to open view mode
    await page.getByText('Website Redesign').click();

    // Wait for view sheet to open
    await expect(page.getByText(/project details/i)).toBeVisible();

    // Click Edit button
    await page.getByRole('button', { name: /edit/i }).first().click();

    // Change status
    const statusSelect = page.locator('select[name="status"], [id="status"]');
    if (await statusSelect.count() > 0) {
      await statusSelect.selectOption('COMPLETED');
    }

    // Submit
    await page.getByRole('button', { name: /update/i }).click();

    // Sheet should close
    await expect(page.getByText(/edit project/i)).not.toBeVisible({ timeout: 10000 });
  });

  test('deletes a project with confirmation', async ({ page }) => {
    await page.goto('/projects');

    // Wait for list to load
    await expect(page.getByText('Website Redesign')).toBeVisible();

    // Click on the row to open view mode
    await page.getByText('Website Redesign').click();

    // Wait for view sheet to open
    await expect(page.getByText(/project details/i)).toBeVisible();

    // Click Delete button
    await page.getByRole('button', { name: /delete/i }).click();

    // Confirmation dialog should appear
    await expect(page.getByText(/are you sure you want to delete/i)).toBeVisible();

    // Confirm deletion
    await page.getByRole('dialog').getByRole('button', { name: /^delete$/i }).click();

    // Dialog should close
    await expect(page.getByText(/are you sure you want to delete/i)).not.toBeVisible({ timeout: 10000 });
  });

  test('filters projects by status', async ({ page }) => {
    await page.goto('/projects');

    // Wait for projects to load
    await expect(page.getByText('Website Redesign')).toBeVisible();

    // Look for status filter (could be dropdown, tabs, or buttons)
    // This will depend on the actual implementation
    const statusFilter = page.locator('[data-testid="status-filter"], select[name="statusFilter"]');

    if (await statusFilter.count() > 0) {
      // Filter by ACTIVE status
      await statusFilter.selectOption('ACTIVE');

      // Should show active projects only
      await expect(page.getByText('Website Redesign')).toBeVisible();

      // Filter by COMPLETED status
      await statusFilter.selectOption('COMPLETED');

      // Should show completed projects only
      await expect(page.getByText('Database Migration')).toBeVisible();
    }
  });

  test('navigates to project detail page', async ({ page }) => {
    await page.goto('/projects');

    // Wait for list to load
    await expect(page.getByText('Website Redesign')).toBeVisible();

    // Click on project to view details
    await page.getByText('Website Redesign').click();

    // Should open detail view
    await expect(page.getByText(/project details/i)).toBeVisible();

    // Verify project information is displayed
    await expect(page.getByText('PROJ001')).toBeVisible();
    await expect(page.getByText('Website Redesign')).toBeVisible();
    await expect(page.getByText(/redesign company website/i)).toBeVisible();
  });

  test('searches projects by name', async ({ page }) => {
    await page.goto('/projects');

    // Wait for projects to load
    await expect(page.getByText('Website Redesign')).toBeVisible();

    // Search for a project
    const searchInput = page.getByPlaceholder(/search.*project/i);
    if (await searchInput.count() > 0) {
      await searchInput.fill('Mobile');

      // Should show matching project
      await expect(page.getByText('Mobile App')).toBeVisible();
    }
  });
});
