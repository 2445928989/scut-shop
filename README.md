# SCUT-Shop

示例全栈电子商务项目（前端 Vue 3 + 后端 Spring Boot），用于开发、测试与教学场景。

主要目录

- `frontend/` — 前端（Vue 3 + Vite + Pinia），含 Playwright E2E 测试。
- `backend/` — 后端（Spring Boot + MyBatis），包含集成测试与 e2e profile。
- `deploy/` — 本地一键启动的 docker-compose 配置。
- `doc/` — 设计、测试与调试文档。

快速开始（推荐）

1. 复制环境变量示例并填写：
   ```bash
   cp .env.example .env
   # 编辑 .env，填写 DB/Redis 等凭据
   ```
2. 启动所有服务：
   ```bash
   docker compose up -d --build
   ```
3. 访问服务：
   - 前端（Nginx）: http://127.0.0.1:3000
   - 健康检查（代理到后端）: http://127.0.0.1:3000/api/health

开发与测试

- 前端本地开发：
  ```bash
  cd frontend
  npm install
  npm run dev
  ```
- 后端本地开发：
  ```bash
  cd backend
  mvn spring-boot:run
  ```
- 运行 E2E（Playwright）：在 `frontend` 目录下运行 `CI=true npm run test:e2e`。

文档与测试

详见 `doc/` 下的文档（包括 `TESTING.md`、`E2E-debugging.md`、`WRITING_GUIDELINES.md` 等）。

贡献流程

建议在分支上提交变更并在 PR 描述中包含复现步骤与测试命令。