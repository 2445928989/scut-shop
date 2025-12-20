# E2E 调试记录（注册 → 登录 → 加入购物车 → 结账）

> 摘要：本次调试目标是让前端 Playwright E2E 测试可靠执行：注册 → 登录 → 加入购物车 → 结账。经过定位与多次运行，发现并修复了多处问题，最终在本地 20/20 次重复运行下全部通过。

## 关键发现

- 间歇性错误表现：有时 UI 显示“已加入购物车”，但服务端购物车为空，导致随后结账返回 `400 { "error": "cart_empty" }`（UI 显示“下单失败”）。
- 根本原因：
  - 前端页面在用户“加入购物车”时立即回退到本地 Pinia store 并显示成功，即使服务端 `POST /api/cart/items` 可能失败或未持久化。页面刷新或导航（SPA 状态丢失）会让 UI 与后端不一致。
  - 另外发现早期有少量请求体缺少 `productId` 或被 403 拦截，但这些问题在后续修复中被消除/覆盖。

## 复现步骤（用于手工验证）

1. 启动后端（e2e 配置，H2）和前端 dev server（vite）。
2. 注册并登录（浏览器 localStorage 会保存 `accessToken`）。
3. 在商品页点击“加入购物车”，观察 Network 中 `POST /api/cart/items`：
   - Request body 应为 `{ "productId": <id>, "quantity": 1 }`。
   - Response 应为 200 且返回 `cartId` 与新增项信息。
4. 访问 /cart，点击“结账”，观察 `POST /api/orders/checkout`：如果 server-side cart 有项，应返回 200。

## 所做改动（总结）

- 前端
  - `frontend/src/pages/Products.vue`：增加日志并确保对 `POST /api/cart/items` 的调用被正确记录（包含请求与响应）。
  - `frontend/src/pages/Cart.vue`：在页面加载时通过 `GET /api/cart` 同步服务端购物车（hydrate），并在 checkout 成功时清理本地 `cartId` 与 Pinia 状态。
  - `frontend/src/stores/cart.ts`：新增 `setItems` 方法以便从服务端覆盖本地 items。
  - `frontend/e2e/auth-e2e.spec.ts`：在测试中明确等待 `POST /api/cart/items` 的响应并断言返回码，以提高健壮性。
  - `frontend/scripts/debug_add_to_cart.js` 与 `frontend/scripts/debug_add_to_cart_repeat.js`：新增调试脚本（单次与重复统计），用于批量运行并生成 `test-results/debug-repeat-*.json` 日志。
  - `.gitignore`：增加 `playwright-report/`、`test-results/`、`.playwright` 等忽略规则。

- 后端
  - 修复若干 SQL / mapper 的兼容性问题（H2 vs MySQL）。
  - `CartController` / `CartService`：确保 `POST /api/cart/items` 在登录/未登录场景下返回 `cartId` 并正确持久化 item。
  - `SecurityConfig`：在 e2e 配置中放行 `/api/cart/**` 与 `/api/products/**`，便于测试访问。

## 运行和统计（执行的命令与结果）

- 复现脚本（单次）：
  - `node scripts/debug_add_to_cart.js` 会生成 `test-results/debug-run-*.json`。
- 复现脚本（重复统计）：
  - `COUNT=20 node scripts/debug_add_to_cart_repeat.js` 会生成 `test-results/debug-repeat-*.json`（包含每次的 request/response、请求体、headers、token 等）。
  - 在修复前，测试显示间歇性失败；修复后 20 次运行统计显示：
    - add-to-cart 成功：20 / 20
    - checkout 成功：20 / 20

建议：将修复提交到分支（例如 `e2e/debug-cart-hydrate`）并创建 PR，PR 标题建议：`fix(e2e): hydrate cart from server & stabilize add-to-cart flow`。
- Playwright E2E：
  - 本地 headless 运行：`CI=true npm run test:e2e`（Playwright 会在 CI=true 下以 headless 模式运行）。
  - Playwright 报告保存在 `frontend/playwright-report/`，并已将其加入 `.gitignore`。

## 关键日志位置

- `frontend/test-results/debug-repeat-<timestamp>.json`（每次 run 的详细事件）
- `frontend/test-results/debug-run-<timestamp>.json`（单次 run 的请求/控制台/对话捕获）
- 如果需要可从 `frontend/playwright-report/index.html` 下载/解包报告（包含 trace/screenshot）。

## 后续建议（短期/长期）

- 短期：在 `addToCart` 的 UX 上进一步改进——**只有在服务端返回 200 并确认 item 存储后再显示“已加入购物车”**，失败时给出明确错误提示或重试选项。
- 长期：将整个流程（`POST /api/cart/items` → `GET /api/cart` → `POST /api/orders/checkout`）纳入集成测试（后端集成测试或 Playwright API Context），并在 CI 中加入重复运行或更严格断言以检测间歇性回归。

## Git / PR 建议

- 建议在分支 `e2e/debug-cart-hydrate` 上提交修复，并创建 PR；建议的 PR 标题：`fix(e2e): hydrate cart from server & stabilize add-to-cart flow`。若需，可在 PR 中附上测试结果与运行截图以便审查。

---