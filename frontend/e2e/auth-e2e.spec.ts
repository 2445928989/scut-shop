import { test, expect, request } from '@playwright/test'

const API_BASE = process.env.API_BASE || 'http://localhost:8080'

function uniqueId() { return `u${Date.now()}${Math.floor(Math.random() * 1000)}` }

test('register -> login -> add to cart -> checkout', async ({ page }) => {
  const username = uniqueId()
  const email = `${username}@example.test`
  const password = 'Password123!'

  // Debug: capture console and network events
  const logs: any[] = []
  const fs = require('fs')
  page.on('console', msg => logs.push({ type: 'console', text: msg.text(), location: msg.location() }))
  page.on('request', req => logs.push({ type: 'request', url: req.url(), method: req.method(), postData: req.postData() }))
  page.on('response', async resp => logs.push({ type: 'response', url: resp.url(), status: resp.status(), headers: resp.headers(), method: resp.request().method() }))
  page.on('requestfailed', req => logs.push({ type: 'requestfailed', url: req.url(), failure: req.failure()?.errorText }))

  try {

    // register via UI (avoid CORS pre-setup)
    await page.goto('/')
    await page.goto('/register')
    await page.fill('text=用户名 >> .. >> input', username)
    await page.fill('text=电子邮件 >> .. >> input', email)
    await page.fill('text=密码 >> .. >> input', password)
    const regRespPromise = page.waitForResponse(resp => resp.url().includes('/api/auth/register'))
    const loginRespPromise = page.waitForResponse(resp => resp.url().includes('/api/auth/login'))
    await page.click('text=注册并登录')
    const regResp = await regRespPromise
    expect([200, 409]).toContain(regResp.status())
    const loginResp = await loginRespPromise
    expect(loginResp.status()).toBe(200)
    // wait for navigation to products/home
    await page.waitForURL('**/')

    // visit products and add first product to cart
    await page.goto('/')
    // wait for product cards
    await page.waitForSelector('text=加入购物车')
    // accept and log alert dialogs from addToCart/checkout
    page.on('dialog', (dialog) => { logs.push({ event: 'dialog', message: dialog.message() }); dialog.accept() })
    const addCartPromise = page.waitForResponse(resp => resp.url().includes('/api/cart/items'))
    await page.click('text=加入购物车')
    const addCartResp = await addCartPromise
    expect(addCartResp.status()).toBe(200)

    // go to cart and checkout (navigate via UI so SPA state is preserved)
    await page.click('text=购物车')
    await page.waitForURL('**/cart')
    await page.waitForTimeout(200)
    const checkoutPromise = page.waitForResponse(resp => resp.url().includes('/api/orders/checkout'))
    await page.click('text=结账')
    await checkoutPromise

    // after checkout, assert cart empty (text appears)
    await expect(page.locator('text=购物车为空')).toBeVisible()

    // also check via API that orders exist for the user
    // get current access token from localStorage
    const token = await page.evaluate(() => localStorage.getItem('accessToken'))
    expect(token).toBeTruthy()
    const apiReq = await request.newContext({ baseURL: API_BASE, extraHTTPHeaders: { Authorization: `Bearer ${token}` } })
    const orders = await apiReq.get('/api/orders')
    expect(orders.ok()).toBeTruthy()
  } finally {
    await fs.promises.mkdir('test-results', { recursive: true })
    await fs.promises.writeFile(`test-results/debug-${Date.now()}.json`, JSON.stringify(logs, null, 2))
  }
})