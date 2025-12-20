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

---

## 一键测试小结 ✅

仓库提供了便捷的一键测试脚本与 Makefile 目标，推荐在本地开发或 CI 中复用：

- `make test-integration` — 在 Docker Compose 的网络中运行后端集成测试（不重建数据库）。
- `make test-integration-clean` — 在执行测试前重建 `scut_shop` 数据库（DROP/CREATE 并重新导入 `db/schema.sql` 与 `db/docker-init.sql`），保证测试以干净的数据库状态开始（注意：此操作会删除测试数据库中的所有数据）。

示例（在仓库根目录）：
```bash
# 在 Compose 网络中运行集成测试（不清理 DB）
make test-integration

# 运行并先清理数据库（CLEAN 模式）
make test-integration-clean
```

> 小贴士：如果你需要运行前端 E2E（Playwright），请在 `frontend` 目录下执行 `CI=true npm run test:e2e`，报告位于 `frontend/playwright-report/`（默认已在 `.gitignore` 中忽略）

---

## 快速开始补充

- 常用命令一览：
  - 启动本地开发环境： `docker compose up -d --build`
  - 后端开发：`cd backend && mvn spring-boot:run`
  - 前端开发：`cd frontend && npm install && npm run dev`
  - 一键集成测试（见上文）：`make test-integration[-clean]`

如果需要，我可以把与 CI 相关的示例工作流（GitHub Actions）也添加到仓库中以便持续集成。