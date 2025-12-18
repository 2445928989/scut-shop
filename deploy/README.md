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
- 若需初始化管理员账号，请运行 SQL 插入语句或提供迁移脚本（我可以帮助生成）。
