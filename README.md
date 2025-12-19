# SCUT-Shop

本仓库为 SCUT Shop 示例全栈应用（前端 + 后端），用于开发与端到端（E2E）测试。

快速导航：

- 前端：`frontend/`（Vue 3 + Vite + Pinia），详见 `frontend/README.md` 获取开发与 Playwright 使用说明。
- 后端：`backend/`（Spring Boot + MyBatis），详见 `backend/README.md` 获取 e2e profile 与集成测试说明。
- 部署：`deploy/` 包含本地开发用的 docker-compose 配置，详见 `deploy/README.md`。
- 文档：`doc/` 包含设计与调试文档。尤其是 `doc/E2E-debugging.md`，记录了本次 E2E 调试过程、发现与复现步骤。

如果需要，我可以把这些文档更新提交为一个 PR，并在 PR 描述中附上调试日志摘要与复现步骤。请告诉我是否需要我发起 PR。