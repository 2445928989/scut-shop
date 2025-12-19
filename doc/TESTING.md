# 测试总览

本文件概述仓库中可用的测试与如何运行它们。

测试类型
- 前端：
  - 单元测试：`vitest`（运行：`npm run test:unit`）
  - E2E：`playwright`（运行：`npm run test:e2e`）
  - 调试辅助：`frontend/scripts/debug_add_to_cart.js` 与 `frontend/scripts/debug_add_to_cart_repeat.js`（详见 `frontend/tests/README.md`）

- 后端：
  - 单元与集成测试：JUnit 5（运行：`mvn test`）
  - E2E profile（H2）：`mvn spring-boot:run -Dspring-boot.run.profiles=e2e`（详见 `backend/TESTING.md`）

产物与查看位置
- Playwright 报告：`frontend/playwright-report/`（打开 `index.html` 可视化查看）
- 调试日志：`frontend/test-results/`（JSON 调试输出）
- 后端测试报告：`backend/target/surefire-reports/`

最佳实践
- 保持 E2E 测试确定性：避免依赖外部网络或不稳定的时间点；推荐在本地使用 `e2e` profile 进行验证。
- 如果怀疑有间歇性失败，使用重复脚本收集统计：
  ```bash
  COUNT=20 npm run debug:add-to-cart:repeat
  ```

另见：
- `doc/E2E-debugging.md` — 详细调试运行手册与本次购物车/结账问题的调查报告。
- `frontend/tests/README.md` — 前端测试与调试工具使用说明。
- `backend/TESTING.md` — 后端测试与 e2e profile 说明。
