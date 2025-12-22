# SCUT-Shop

一个简化的全栈在线购物演示（前端 Vue 3 + 后端 Spring Boot）。本仓库已缩减为最小 demo，以便在本地快速启动与调试。

快速开始（最小演示）

1. 复制环境变量并编辑：
   ```bash
   cp .env.example .env
   # 按需编辑 .env（主要是 DB/邮件相关配置）
   ```
2. 使用 Docker Compose 启动（单条命令）：
   ```bash
   docker compose up -d --build
   ```
3. 访问服务：
   - 前端（Nginx）: http://127.0.0.1:3000
   - MailHog（如启用）: http://127.0.0.1:8025

注意：本分支移除了复杂的自动化脚本、Playwright E2E 和部分历史迁移脚本，仓库保留最必要的源码与 `docker-compose.yml` 以便快速演示与开发。若需恢复完整历史或测试，请在对应分支或 Git 历史中查找。

开发模式

- 前端开发：
  ```bash
  cd frontend
  npm install
  npm run dev
  ```
- 后端开发：
  ```bash
  cd backend
  mvn spring-boot:run
  ```

问题排查（简要）

- 检查容器状态： `docker compose ps`
- 前端健康检查（代理至后端）： `curl -i http://localhost:3000/api/health`

---


## 集成测试（说明）

为使本分支保持为一个最小演示（demo），仓库已移除一键测试脚本与 Makefile 目标。

如需运行集成测试，请在 `backend/` 目录使用 Maven（或在 CI 中设置专用 job）：

```bash
cd backend && mvn -Dtest=IntegrationTest test
```

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
