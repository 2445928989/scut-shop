# 后端测试与 e2e 配置

本文件说明如何运行后端相关测试以及用于集成测试的 e2e profile。

集成测试
- 单元与集成测试位于 `src/test/java/`，使用 JUnit 5。
- 示例集成测试（购物车与结账流）位于：`src/test/java/com/scutshop/backend/IntegrationTest.java`。

运行测试
- 运行所有单元与集成测试：
  ```bash
  cd backend
  mvn test
  ```

- 运行特定集成测试（示例）：
  ```bash
  mvn -Dtest=IntegrationTest test
  ```

E2E profile（H2）
- 为了快速在本地验证 E2E 流程，提供了 `e2e` Spring profile，使用内存 H2 数据库并由 `src/main/resources/db/h2-init.sql` 进行初始化。
- 使用 e2e profile 启动后端：
  ```bash
  cd backend
  mvn spring-boot:run -Dspring-boot.run.profiles=e2e
  ```
- 该 profile 允许 Playwright 测试调用 `/api/cart/**`、`/api/products/**` 等接口，便于本地自动化验证。

## 在 compose 网络中运行集成测试（说明） 🔧

为保持此分支为最小演示，早期用于在 compose 网络中运行集成测试的脚本和 Makefile 目标已被移除（这些工具可以在历史分支或完整分支中找到）。

如果需要在本地运行集成测试，请在 `backend/` 目录中使用 Maven（或在 CI 中设置专用 job）：

```bash
# 运行集成测试（在 backend 中）
cd backend && mvn -Dtest=IntegrationTest test
```

如需在 compose 网络中运行测试，请在非 demo 分支恢复对应脚本或在 CI 中创建相应的 job。

注意
- 若需可复现的数据库状态以进行集成测试，推荐使用 `src/test/resources/db/schema.sql`（测试资源），或在测试中使用 Testcontainers 来模拟真实的 MySQL 环境。
