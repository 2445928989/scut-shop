# Docker 部署说明 (开发环境)

该示例使用 `docker-compose` 来启动以下服务：
- MySQL 8（初始化会执行 `db/schema.sql` 与 `db/docker-init.sql`）
- Redis
- MailHog（用于捕获开发邮件）
- Adminer（数据库管理界面）
- app（占位，需在 `backend/` 下创建后端工程）

## 快速开始
1. 复制示例 env：
   cp .env.example .env
   并根据需要修改密码和配置。

2. 启动服务：
   docker compose up -d

3. 检查服务：
   - MySQL: 3306
   - Adminer: http://localhost:8080
   - MailHog UI: http://localhost:8025
   - App (示例): http://localhost:8081

## 注意事项
- `.env` 中包含敏感信息，请不要提交到远程仓库（已将 `.env.example` 提交为示例）。
- `db/schema.sql` 会在 MySQL 初次启动时被执行以创建数据表；如果你已启动过容器，修改 schema 需通过迁移工具（Flyway / Liquibase）或手动同步。
- 数据库初始化脚本 `db/init.sql` 会自动创建默认管理员账户（用户名：`admin`，邮箱：`admin@shop.local`，密码：`Admin@2024`）。
- 如果你需要在容器已初始化后重新创建管理员，请手动运行相应的 SQL 插入语句或使用迁移工具。

---

## E2E 测试说明

- 本仓库包含前端 Playwright E2E 测试与调试脚本（在 `frontend/` 下）。用于调试的脚本会产生日志在 `frontend/test-results/`，Playwright 报告位于 `frontend/playwright-report/`（这两个目录已加入 `.gitignore`）。
- 后端有一个 e2e profile（H2）用于本地快速运行集成场景，参见 `backend/README.md` 与 `doc/E2E-debugging.md` 获取更详细说明和复现步骤。
