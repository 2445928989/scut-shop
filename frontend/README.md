# SCUT Shop - 前端

前端使用 Vue 3 + Vite + TypeScript + Pinia，包含 UI、API 客户端封装与 Playwright E2E 测试。

快速开发

```bash
cd frontend
npm install
npm run dev
```

API 配置（默认）

- 默认使用相对路径（同源）访问后端 API。若需覆盖可在构建或运行时通过 `VITE_API_BASE` 注入自定义基地址。
- Dockerfile 中包含 `ARG VITE_API_BASE=/api`，用于在镜像构建时注入默认值。

构建与运行（Docker Compose）

```bash
# 构建并启动 frontend 服务（仓库根目录）
docker compose build frontend
docker compose up -d frontend
```

E2E（Playwright）

- 安装浏览器（建议使用国内镜像以加速）：
  ```bash
  npm run playwright:install:mirror
  ```
- 在 CI（headless）模式运行：
  ```bash
  CI=true npm run test:e2e
  ```
- 在本地以有界面模式运行：
  ```bash
  npm run test:e2e:headed
  ```

调试脚本

- 单次运行： `npm run debug:add-to-cart`（输出到 `frontend/test-results/`）
- 批量统计： `COUNT=20 npm run debug:add-to-cart:repeat`（生成重复运行统计）

产物与日志

- Playwright HTML 报告：`frontend/playwright-report/index.html`（已加入 `.gitignore`）
- 调试 JSON：`frontend/test-results/`（已加入 `.gitignore`）

注意

- 在 UI 操作（如“加入购物车”）时，应等待服务端返回成功再更新界面，避免本地与服务端状态不一致。

