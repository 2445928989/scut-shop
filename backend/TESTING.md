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

## 在 compose 网络中运行集成测试（推荐） 🔧

- 仓库根目录提供脚本 `scripts/run-integration-tests.sh` 和 Makefile 目标 `test-integration` / `test-integration-clean`，用于在 Docker Compose 的网络中运行集成测试（在 Maven 官方镜像中执行测试，连接到 `scut_mysql` 等服务）。

- 用法示例：
  ```bash
  # 运行集成测试（不重建 DB）
  make test-integration

  # 运行集成测试并先重建数据库（CLEAN 模式，会删除并重新创建测试 DB 并重新导入 schema/init 数据）
  make test-integration-clean
  ```

- 注意：CLEAN 模式会在 `scut_mysql` 容器中执行 DROP/CREATE，并重新应用 `db/schema.sql` 与 `db/docker-init.sql`，会删除现有测试数据库中的数据，请谨慎使用。

注意
- 若需可复现的数据库状态以进行集成测试，推荐使用 `src/test/resources/db/schema.sql`（测试资源），或在测试中使用 Testcontainers 来模拟真实的 MySQL 环境。
