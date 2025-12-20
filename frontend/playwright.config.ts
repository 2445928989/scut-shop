import { defineConfig, devices } from '@playwright/test'

export default defineConfig({
  testDir: './e2e',
  timeout: 60_000,
  expect: { timeout: 5000 },
  fullyParallel: false,
  retries: process.env.CI ? 1 : 0,
  reporter: [['list'], ['html', { outputFolder: 'playwright-report' }]],
  use: {
    baseURL: process.env.FRONTEND_BASE || 'http://localhost:3000',
    // Allow forcing headless via environment variable (useful on CI or headless hosts)
    headless: !!process.env.PLAYWRIGHT_HEADLESS || !!process.env.CI,
    viewport: { width: 1280, height: 720 },
    ignoreHTTPSErrors: true,
  },
  projects: [
    { name: 'chromium', use: { ...devices['Desktop Chrome'] } }
  ],
  // Start the dev server before tests (if not already running)
  webServer: {
    command: 'npm run dev',
    url: 'http://localhost:3000',
    // In CI we normally start the server, but in local test runs reuse the existing frontend
    reuseExistingServer: true,
    timeout: 120_000
  }
})