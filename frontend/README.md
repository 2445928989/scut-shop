# SCUT Shop - 前端

This is a minimal scaffold for the SCUT Shop frontend using Vue 3 + Vite + TypeScript + Element Plus + Pinia.

快速启动：

1. cd frontend
2. npm install
3. npm run dev

环境变量：
- API 地址：如果后端在其他主机，请在 `.env` 中设置 `VITE_API_BASE`。

备注：
- 当前为示例脚手架，生产环境需补充完善校验、错误处理与 token 刷新逻辑。

Playwright 国内镜像（加速下载）：

- 单次（临时使用）：

  ```bash
  PLAYWRIGHT_DOWNLOAD_HOST=https://npmmirror.com/mirrors/playwright \
  PLAYWRIGHT_DOWNLOAD_CONNECTION_TIMEOUT=120000 \
  npx playwright install
  ```

- 推荐使用 npm 脚本（跨平台）：

  ```bash
  npm run playwright:install:mirror
  ```

- Permanent for your shell (all future installs): add to your `~/.bashrc` or `~/.zshrc`:

  ```bash
  export PLAYWRIGHT_DOWNLOAD_HOST=https://npmmirror.com/mirrors/playwright
  export PLAYWRIGHT_DOWNLOAD_CONNECTION_TIMEOUT=120000
  ```

- Optional: set shared browsers cache for multiple machines:

  ```bash
  export PLAYWRIGHT_BROWSERS_PATH="$HOME/pw-browsers"
  ```

以上设置能加速 Playwright 在国内的浏览器下载，并适用于 CI 场景。若需要，我可以添加一个小脚本来自动配置这些环境变量。

## E2E / 调试（Playwright）

- 启动开发服务器：

  ```bash
  cd frontend
  npm install
  npm run dev
  ```

- 安装 Playwright（建议使用镜像以加速）：

  ```bash
  npm run playwright:install:mirror
  ```

- 在 headless（CI）模式运行 Playwright 测试：

  ```bash
  CI=true npm run test:e2e
  ```

- 在本地以有界面模式运行：

  ```bash
  npm run test:e2e:headed
  ```

- 调试脚本（用于复现与收集日志）：
  - 单次运行（捕获网络/控制台/对话事件）：

    ```bash
    npm run debug:add-to-cart
    # 输出：frontend/test-results/debug-run-<timestamp>.json
    ```

  - 批量统计运行（默认 20 次）：

    ```bash
    npm run debug:add-to-cart:repeat
    # 或： COUNT=20 npm run debug:add-to-cart:repeat
    # 输出：frontend/test-results/debug-repeat-<timestamp>.json
    ```

- 产物与日志：
  - Playwright HTML 报告：`frontend/playwright-report/index.html`
  - 调试日志与统计：`frontend/test-results/`

- 注意与最佳实践：
  - 在 UI 上显示“已加入购物车”之前，应等待服务端确认 `POST /api/cart/items` 成功；页面导航或刷新后应调用 `GET /api/cart` 来同步（hydrate）。
  - `playwright-report/` 与 `test-results/` 已加入 `.gitignore`，请勿将测试产物提交到仓库。

- 更多详情请参阅仓库中的 `doc/E2E-debugging.md`。

## 测试结构（文件位置说明）

- `frontend/e2e/` - Playwright 测试（E2E 流程的权威实现）
- `frontend/scripts/` - 调试辅助脚本（单次复现与批量统计）
- `frontend/test-results/` - 调试输出与 JSON（已加入 `.gitignore`）
- `frontend/playwright-report/` - Playwright 可视化报告（已加入 `.gitignore`）

常用 npm 快捷命令：

- `npm run debug:add-to-cart` — 运行单次调试脚本（生成 `test-results/debug-run-*.json`）。
- `npm run debug:add-to-cart:repeat` — 运行批量统计脚本（默认 20 次，生成 `test-results/debug-repeat-*.json`）。

