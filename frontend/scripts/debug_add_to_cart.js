const { chromium } = require('playwright');
const fs = require('fs');

(async () => {
    const logs = [];
    const browser = await chromium.launch({ headless: true });
    const context = await browser.newContext();
    const page = await context.newPage();

    page.on('console', msg => logs.push({ type: 'console', text: msg.text(), location: msg.location() }));
    context.on('request', req => logs.push({ type: 'request', url: req.url(), method: req.method(), postData: req.postData() }));
    context.on('response', async resp => logs.push({ type: 'response', url: resp.url(), status: resp.status(), ok: resp.ok() }));
    context.on('requestfailed', req => logs.push({ type: 'requestfailed', url: req.url(), failure: req.failure() ? req.failure().errorText : undefined }));

    try {
        const base = process.env.FRONTEND_BASE || 'http://localhost:3000';
        await page.goto(base + '/register', { waitUntil: 'networkidle' });
        const username = `u${Date.now()}${Math.floor(Math.random() * 1000)}`;
        const email = `${username}@example.test`;
        const password = 'Password123!';

        await page.fill('text=用户名 >> .. >> input', username);
        await page.fill('text=电子邮件 >> .. >> input', email);
        await page.fill('text=密码 >> .. >> input', password);

        const regRespPromise = page.waitForResponse(resp => resp.url().includes('/api/auth/register'));
        const loginRespPromise = page.waitForResponse(resp => resp.url().includes('/api/auth/login'));
        await page.click('text=注册并登录');
        const regResp = await regRespPromise;
        const loginResp = await loginRespPromise;
        logs.push({ event: 'registerResponse', status: regResp.status() });
        logs.push({ event: 'loginResponse', status: loginResp.status() });

        await page.waitForURL('**/');
        await page.goto(base + '/', { waitUntil: 'networkidle' });

        // wait for product card
        try {
            await page.waitForSelector('text=加入购物车', { timeout: 5000 });
            logs.push({ event: 'foundAddToCart' });
            // accept any dialog
            page.on('dialog', dlg => { logs.push({ event: 'dialog', message: dlg.message() }); dlg.accept(); });
            await page.click('text=加入购物车');
            logs.push({ event: 'clickedAddToCart' });
            // give a moment for network
            await page.waitForTimeout(500);
        } catch (e) {
            logs.push({ event: 'noAddToCart', error: e.message });
        }

        await page.goto(base + '/cart', { waitUntil: 'networkidle' });
        const cartText = await page.textContent('main') || '';
        logs.push({ event: 'cartContent', text: cartText.trim() });

        // capture localStorage token
        const token = await page.evaluate(() => localStorage.getItem('accessToken'));
        logs.push({ event: 'accessToken', token });

        // save logs
        await fs.promises.mkdir('test-results', { recursive: true });
        const out = `test-results/debug-run-${Date.now()}.json`;
        await fs.promises.writeFile(out, JSON.stringify(logs, null, 2));
        console.log('Wrote', out);
    } catch (err) {
        console.error('Error during debug run', err);
    } finally {
        await browser.close();
    }
})();