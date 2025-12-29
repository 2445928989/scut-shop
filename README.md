# SCUT-Shop

王思哲 网络工程班 202330451741

网络应用架构设计与开发大作业：全栈在线购物网站（前端 Vue 3 + 后端 Spring Boot）。

快速开始

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
   - Adminer（数据库管理）: http://127.0.0.1:8080
   - 后端 API（映射端口）: http://127.0.0.1:8081
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

## 快速开始补充

- 常用命令一览：
  - 启动本地开发环境： `docker compose up -d --build`
  - 后端开发：`cd backend && mvn spring-boot:run`
  - 前端开发：`cd frontend && npm install && npm run dev`
