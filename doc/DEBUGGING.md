# 调试记录 — 完整 E2E（注册→登录→加入购物车→结账）整合与 CORS 问题

- 日期：2025-12-20
- 目的：把前后端与依赖（MySQL/Redis/Mailhog）整合到 `docker compose up`，并让浏览器端从 `http://localhost:3000` 能完成完整认证流（注册→登录→加购物车→结账）。

---

## 1) 问题概述 ✅
- 浏览器 UI 中 **注册 / 登录失败**，控制台报错：
  - `Response to preflight request doesn't pass access control check: Redirect is not allowed for a preflight request.`
  - Axios 抛 `Network Error` / `net::ERR_FAILED`
- 但通过 `curl http://127.0.0.1:3000/api/...`（由 nginx 代理到后端）可以成功调用后端接口。

**结论**：浏览器端的 JS 发起了跨域请求到后端内部主机（例如 `http://app:8080`），导致预检（OPTIONS）被阻断或返回重定向（3xx），触发 CORS 错误。

---

## 2) 关键发现与修改 ✨
- 发现：构建产物中或代码里存在绝对 API 地址（`http://app:8080` / `http://localhost:8081`）。
- 修改：把前端的默认 API base 改为**相对路径（同源）**，并将 refresh 请求改为同源路径：
  - `frontend/src/api/index.ts`：默认 `API_BASE` 改为 `''`（相对），并将 refresh 请求改为 `axios.post('/api/auth/refresh', ...)`。
  - `frontend/src/stores/auth.ts`：将 refresh 使用 `axios.post('/api/auth/refresh', ...)`（同源）以避免构造绝对 URL。
- 保持 nginx 代理配置：`frontend/docker/nginx.conf` 通过 `location /api/` 代理到 `http://app:8080/api/`。
- 保持 docker-compose 前端 build arg：`VITE_API_BASE=/api`（仍可覆盖，但默认使用相对路径更安全）。

---

## 3) 关键文件变更（已提交）
- **修改**
  - `frontend/src/api/index.ts` — 改为同源默认、refresh 使用 `/api`。
  - `frontend/src/stores/auth.ts` — 改为同源 refresh。
- **已存在/验证**
  - `frontend/Dockerfile` — `ARG VITE_API_BASE=/api`，multi-stage build（node -> nginx）。
  - `frontend/docker/nginx.conf` — `location /api/ { proxy_pass http://app:8080/api/; ... }`。
  - `docker-compose.yml` — frontend 服务已加入（3000:80）。

---

## 4) 执行过的重要命令与观测 🧭
- 在宿主机：
  - `curl -v http://127.0.0.1:3000/` → 返回 `index.html`（前端 nginx 成功）
  - `curl -v http://127.0.0.1:3000/api/health` → HTTP 200（nginx 代理到 backend 成功）
  - `curl -X POST http://127.0.0.1:3000/api/auth/register ...` → 成功（后端收到注册请求）
- 容器内检查构建产物：
  - 在 `scut_frontend` 容器中 grep JS，确认是否有 `app:8080`／绝对 host 的残留（之前有查找并确认构建中可能含有绝对地址）。
- 尝试重建：
  - `docker compose build --no-cache frontend && docker compose up -d frontend`
  - 遇到网络拉取基础镜像超时（`failed to fetch oauth token`），需要先确保宿主机能拉取 `node:18-alpine` / `nginx:stable-alpine`，或手动 `docker pull`。

---

## 5) 验证步骤（如何复现 / 验证）🔍
1. 构建并启动：
   - `docker compose build --no-cache frontend && docker compose up -d frontend`
2. 浏览器访问：
   - 打开 `http://127.0.0.1:3000` → 在 DevTools 的 Network 面板中观察注册/登录请求
   - 期望：请求 URL 为 `http://127.0.0.1:3000/api/auth/register` 或相对 `/api/auth/register`，OPTIONS（preflight）返回 200/204 并且无 3xx 重定向。
3. 如果成功，继续 E2E 或手动完成注册→登录→加入购物车→结账。可运行 Playwright 测试：`npm run e2e`（或 CI 配置）

---

## 6) 未完成 / 后续动作（TODO）📝
- [ ] 成功重建 frontend 镜像（注意：若网络拉取基础镜像失败可先 `docker pull node:18-alpine nginx:stable-alpine`）。
- [ ] 在浏览器上验证预检（OPTIONS）是否通过，如仍失败采集 Network 中的 OPTIONS 请求/响应头并附上 nginx + backend 日志供进一步分析。
- [ ] 如 OPTIONS 返回被 nginx 重定向，调整 `nginx.conf` 或后端 CORS 配置以正确响应 OPTIONS（不重定向，返回必要 CORS headers）。

---

## 7) 相关日志/错误摘录
- 浏览器错误（Network console）示例：
  - `Response to preflight request doesn't pass access control check: Redirect is not allowed for a preflight request.`
- Docker build 错误：
  - `failed to fetch oauth token: ... dial tcp ... i/o timeout`（网络拉取镜像超时）
- 后端日志（有 CorsFilter）：
  - Spring Boot 启动日志中包含 `org.springframework.web.filter.CorsFilter` 和 `HandlerMappingIntrospector` 的消息（用于 CORS 查找）。

---

## 8) 记录与后续说明
- 本文档记录了本次主要发现与后续建议。如需，可将其整理为 PR 描述或 CI 检查项以便在流程中复现与跟踪。

---

如需将该文档转换为其他格式或将其提交到仓库，请在 PR 或 issue 中说明变更需求并附上相关日志或补充信息。
