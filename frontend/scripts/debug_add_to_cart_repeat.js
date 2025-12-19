const { chromium } = require('playwright');
const fs = require('fs');

(async () => {
    const COUNT = parseInt(process.env.COUNT || '20', 10);
    const base = process.env.FRONTEND_BASE || 'http://localhost:3000';
    const stats = {
        runs: COUNT,
        addCart: { success: 0, missingProductId: 0, forbidden: 0, otherErrors: 0 },
        checkout: { success: 0, cart_empty: 0, other: 0 },
        details: []
    };

    for (let i = 0; i < COUNT; i++) {
        const logs = [];
        const browser = await chromium.launch({ headless: true });
        const context = await browser.newContext();
        const page = await context.newPage();

        page.on('console', msg => logs.push({ type: 'console', text: msg.text(), location: msg.location() }));
        context.on('request', req => logs.push({ type: 'request', url: req.url(), method: req.method(), postData: req.postData() }));
        context.on('response', async resp => logs.push({ type: 'response', url: resp.url(), status: resp.status(), ok: resp.ok() }));
        context.on('requestfailed', req => logs.push({ type: 'requestfailed', url: req.url(), failure: req.failure() ? req.failure().errorText : undefined }));

        try {
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
            await regRespPromise;
            await loginRespPromise;

            await page.waitForURL('**/');

            await page.goto(base + '/', { waitUntil: 'networkidle' });
            // wait for product card
            try {
                await page.waitForSelector('text=加入购物车', { timeout: 5000 });
            } catch (e) {
                logs.push({ event: 'noAddToCart', error: e.message });
            }

            // listen for add-to-cart response specifically
            let addResp = null;
            const addPromise = page.waitForResponse(resp => resp.url().includes('/api/cart/items'))
                .then(r => { addResp = r; return r; })
                .catch(e => { /* ignore */ });

            page.on('dialog', dlg => { logs.push({ event: 'dialog', message: dlg.message() }); dlg.accept(); });
            await page.click('text=加入购物车');

            // wait briefly for network to settle
            try { await addPromise; } catch (e) { /* ignore */ }
            await page.waitForTimeout(300);

            // analyze add response
            const detail = { run: i + 1, add: null, checkout: null, logs };
            const currentToken = await page.evaluate(() => localStorage.getItem('accessToken'));
            if (!addResp) {
                // no response captured
                detail.add = { status: 'no_response', token: currentToken };
                stats.addCart.otherErrors++;
            } else {
                const status = addResp.status();
                const request = addResp.request();
                let postData = null;
                try { postData = request.postData(); } catch (e) { /* ignore */ }
                const hasProductId = postData && postData.includes('productId');
                let body = null;
                try { body = await addResp.text(); } catch (e) { /* ignore */ }
                const reqHeaders = request.headers ? request.headers() : {};
                const respHeaders = addResp.headers ? addResp.headers() : {};
                detail.add = { status, postData, body, reqHeaders, respHeaders, token: currentToken };

                if (status === 200) {
                    stats.addCart.success++;
                } else if (!hasProductId) {
                    stats.addCart.missingProductId++;
                } else if (status === 403) {
                    stats.addCart.forbidden++;
                } else {
                    stats.addCart.otherErrors++;
                }
            }

            // go to cart and try to checkout if add reported success
            await page.goto(base + '/cart', { waitUntil: 'networkidle' });
            let checkoutResp = null;
            try {
                // wait briefly for checkout button to appear
                await page.waitForSelector('text=结账', { timeout: 2000 });
                const checkoutPromise = page.waitForResponse(resp => resp.url().includes('/api/orders/checkout')).catch(() => null);
                await page.click('text=结账');
                checkoutResp = await checkoutPromise;
            } catch (e) {
                // no checkout button or timing out — capture cart content
                const cartText = await page.textContent('main').catch(() => null);
                detail.checkout = { status: 'no_checkout_button', text: cartText && cartText.trim() };
                stats.checkout.other++;
            }

            if (checkoutResp) {
                const status = checkoutResp.status();
                const body = await checkoutResp.text();
                detail.checkout = { status, body };
                if (status === 200) stats.checkout.success++; else if (body && body.includes('cart_empty')) stats.checkout.cart_empty++; else stats.checkout.other++;
            }

            stats.details.push(detail);

        } catch (err) {
            console.error('Error during run', i + 1, err);
            stats.details.push({ run: i + 1, error: (err && err.message) || String(err) });
        } finally {
            await browser.close();
        }
    }

    // write results
    await fs.promises.mkdir('test-results', { recursive: true });
    const out = `test-results/debug-repeat-${Date.now()}.json`;
    await fs.promises.writeFile(out, JSON.stringify(stats, null, 2));
    console.log('Wrote', out);
    console.log('Summary:', JSON.stringify({ addCart: stats.addCart, checkout: stats.checkout }, null, 2));

})();