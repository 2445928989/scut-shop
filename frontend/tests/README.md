# 前端测试与调试工具说明

本文件说明前端测试与调试脚本的位置以及如何运行。

位置说明
- `frontend/e2e/` — Playwright 测试（作为 E2E 流程的权威来源）
- `frontend/scripts/` — 调试辅助脚本（快速复现与批量统计）
- `frontend/test-results/` — 输出日志与 JSON（已加入 `.gitignore`）
- `frontend/playwright-report/` — Playwright 可视化报告（已加入 `.gitignore`）

如何运行
- 启动开发服务器：
  ```bash
  cd frontend
  npm install
  npm run dev
  ```

- 运行 Playwright 测试套件（headless）：
  ```bash
  CI=true npm run test:e2e
  ```

- 运行单次调试脚本：
  ```bash
  npm run debug:add-to-cart
  ```

- 运行批量统计调试脚本：
  ```bash
  npm run debug:add-to-cart:repeat
  # 或覆盖 COUNT 环境变量
  COUNT=50 npm run debug:add-to-cart:repeat
  ```

注意事项
- 调试脚本会在 `frontend/test-results/` 生成文件，包含网络请求、控制台日志、对话框捕获与统计信息。
- 请勿将 `playwright-report/` 与 `test-results/` 提交到代码仓库（已在 `.gitignore` 配置）。
