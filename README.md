# SCUT-Shop

全栈在线购物网站（前端 Vue 3 + 后端 Spring Boot）。

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
- 运行 E2E（Playwright）：（已从简化演示中移除。如需完整 E2E，请在非 demo 分支启用 Playwright。）

激活演示（POC）：
- 若要在本地启用邮件激活，请在后端服务中设置 `EMAIL_ACTIVATION_ENABLED=true`，并确保 MailHog 可以通过 `http://localhost:8025` 访问以捕获外发邮件（Compose 已包含 MailHog 服务）。  
- 可使用环境变量 `FRONTEND_BASE` 来自定义激活链接的主机（默认 `http://localhost:3000`）。

文档与测试

详见 `doc/` 下的文档（包括 `TESTING.md`、`E2E-debugging.md`、`WRITING_GUIDELINES.md` 等）。

贡献流程

建议在分支上提交变更并在 PR 描述中包含复现步骤与测试命令。

---

## 一键测试 ✅

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

---

## 手动测试与排查指南 🛠️

下面说明在本地以浏览器或命令行验证服务、测试邮箱激活流程以及常见问题的排查命令。按照顺序进行可以快速定位问题。

### 前置检查

- 确认容器在运行：
  ```bash
  docker compose ps
  ```
- 常用端口：
  - 前端（Nginx）： http://localhost:3000
  - 后端（映射端口）： http://localhost:8081
  - MailHog UI： http://localhost:8025
  - Adminer： http://localhost:8080

### 启动与重启

- 启动所有服务：
  ```bash
  docker compose up -d --build
  ```
- 重启单个服务（示例 Adminer）：
  ```bash
  docker restart scut_adminer
  ```

### 验证服务

- 通过前端代理验证后端：
  ```bash
  curl -i http://localhost:3000/api/health
  ```
  （推荐，能绕过直接访问后端根路径导致的 403 误解）

- 直接请求后端 API（注意 CORS/Origin）：
  ```bash
  curl -i -H "Origin: http://localhost:3000" http://localhost:8081/api/health
  ```
  > 说明：直接在浏览器地址栏打开 `http://localhost:8081` 可能显示 403，这是后端仅作为 API 的默认行为，不影响通过前端或带合适 Origin 的请求访问 API。

- MailHog 邮件查看：
  - UI： http://localhost:8025
  - API：
    ```bash
    curl 'http://localhost:8025/api/v2/messages' | jq '.'
    ```

- Adminer 登录：
  - 地址： http://localhost:8080
  - 登录示例：
    - System: MySQL
    - Server: `db` （容器内）或 `127.0.0.1:3307`（宿主映射）
    - Username/Password: `root`/`123456789` 或 `scut_user`/`123456`
    - Database: `scut_shop`（可选）

### 邮件激活流程（若启用）

- 启用激活：在服务环境中设置 `EMAIL_ACTIVATION_ENABLED=true`（Compose 默认为 false，为了开箱即用）。
- 激活链接前缀：使用 `FRONTEND_BASE` 环境变量来指定前端主机（默认 `http://localhost:3000`）。
- 邮件发送相关环境变量（示例）：
  ```env
  MAIL_HOST=mailhog
  MAIL_SMTP_PORT=1025
  MAIL_USERNAME=
  MAIL_PASSWORD=
  MAIL_FROM="SCUT Shop <no-reply@example.com>"
  MAIL_TLS=false   # true for STARTTLS (587), false when using SSL (465)
  ```
- 手动测试步骤：
  1. 在前端页面注册（http://localhost:3000 → 注册）。
  2. 在 MailHog UI 查找激活邮件并点开激活链接（或复制 token）。
  3. 使用浏览器或直接调用后端激活接口：
     ```bash
     curl -i "http://localhost:8081/api/auth/activate?token=<token>"
     ```

### Playwright E2E（本地/CI）

- 在宿主运行测试（使用本地端口）：
  ```bash
  cd frontend
  CI=true API_BASE=http://localhost:8081 MAILHOG_API=http://localhost:8025/api/v2/messages npx playwright test
  ```
- 在容器內运行测试（服务名可用，适合在 compose 网络中执行）：
  ```bash
  # 在 frontend 容器內执行，使用服务名 app 和 mailhog
  CI=true API_BASE=http://app:8080 MAILHOG_API=http://mailhog:8025/api/v2/messages npx playwright test
  ```
- 提示：如果测试在宿主上运行出现 `getaddrinfo EAI_AGAIN app` 类似 DNS 问题，改用 `localhost` 的 API_BASE 可解决；在容器里运行测试可直接使用服务名。

### 常见故障与排查命令

- 后端返回 403 或根路径不可用：
  - 原因：后端是 API 服务，根路径並不提供 UI；使用前端代理或带 Origin 头的请求来验证 API。
  - 验证示例：
    ```bash
    curl -i -H "Origin: http://localhost:3000" http://localhost:8081/api/auth/register -d '{"username":"u1","email":"u1@example.test","password":"Password123"}' -H 'Content-Type: application/json'
    ```

- MailHog 返回不稳定或 JSON 解析失败：
  - 使用 API 路径 `/api/v2/messages`（不要仅请求根 `/`）。
  - 如出现问题，重启 MailHog： `docker restart scut_mailhog`，并查看日志： `docker logs scut_mailhog --since 1m | sed -n '1,200p'`

- Adminer 无响应或连接被重置：
  - 重启容器： `docker restart scut_adminer`，检查日志： `docker logs scut_adminer --tail 200`
  - 尝试使用宿主映射端口 `127.0.0.1:3307` 连接 MySQL（当直接用容器内主机名连接失败时）

- 查看关键日志和状态：
  ```bash
  docker logs scut_app --since 1m | sed -n '1,200p'
  docker logs scut_mailhog --since 1m | sed -n '1,200p'
  docker logs scut_adminer --tail 200
  docker exec scut_mysql mysqladmin ping -u root -p123456789
  docker exec scut_mysql mysql -u root -p123456789 -e "SELECT id, username, email, status FROM scut_shop.user LIMIT 5;"
  ```

### CI 建议

- 推荐建立两个并行的 E2E Job：
  - Job A（activation=true）：验证邮件激活流程（Playwright 激活测试）。
  - Job B（activation=false）：验证注册后自动登录并完成购物流程（auth-e2e）。
- 另一种可选策略是让测试根据注册接口返回的行为自适应（如果返回 `{ activation: 'sent' }` 则执行激活步骤），从而减少环境切换。

---
