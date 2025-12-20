# 后端（Spring Boot + MyBatis）

后端实现用户认证、商品、购物车与订单等核心 API，以下说明用于本地开发与测试。

快速启动（本地，使用仓库根目录的 docker-compose）：

1. 复制 `.env.example` 为 `.env` 并填写数据库凭据。
2. 构建并启动服务：
   docker compose up -d --build

后端服务将通过代理暴露在 `http://localhost:8081`（容器内部端口 8080）。

示例接口：
- GET `/api/health` - 健康检查
- POST `/api/auth/register` - 注册（请求体：`{username,email,password}`）

后续建议：
- 实现 JWT 鉴权与登录接口
- 对复杂查询采用 MyBatis XML 或注解方式
- 添加单元与集成测试

## E2E / e2e 配置（H2）

- 使用 e2e 模式运行后端（会使用内存 H2 数据库并加载初始数据）：

  ```bash
  # 在仓库根目录下运行
  cd backend
  mvn spring-boot:run -Dspring-boot.run.profiles=e2e
  ```

- e2e profile 会加载 `src/main/resources/db/h2-init.sql`，并允许前端在测试期间调用 `/api/cart/**`、`/api/products/**` 等接口。
- 集成测试示例：`src/test/java/com/scutshop/backend/IntegrationTest.java`，演示了购物车与结账的程序化调用。
- 调试步骤与脚本请参见 `doc/E2E-debugging.md`。

