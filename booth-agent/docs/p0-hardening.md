# P0/P1 增量加固（2026-10-31）

> **口径（2026-11-14）**：写操作已改为**对话内直写**；确认卡 / `/app/chat/confirm` 废弃。下文「确认票」若仍出现，视为历史实现细节；对外以「指纹 + outbox 幂等 + 写熔断」为准。本文为历史进度摘要，以边界文档 + README + 源码为准。

## 已落地

1. **参数指纹**：同人同参去重；`ParamFingerprints` MD5（直写与历史确认票共用算法）。
2. **写操作 outbox**：`write_action_outbox` 表；直写路径 PENDING→SUCCESS/FAILED/NEED_RECONCILE；成功可按指纹幂等回放。
3. **用户限流**：Redis Lua（并发 + 每分钟配额），挂在 `/app/chat` 与 `/stream`。
4. **工具超时预算**：`ToolExecutor` 按 `governance.tool-timeout` 截断。
5. **工具元数据**：`readOnly` 等；写工具经 `WriteActionService.executeDirect`。
6. **槽位**：`SlotFiller` 统一抽订单号；`NEED_ORDER_SN` 下纯单号回填售后。
7. **评测指标**：`GoldenMetricsReportTest` 写出 `target/golden-metrics.json`。
8. **安全小补**：`UntrustedText` 隔离；GoldenSet 覆盖越权查单；写操作审计落 PG。
9. **Compose**：`docker compose up -d`（或 `--build`）默认起 pgvector(5433)+Redis(6380)+Agent。

## 简历可写（须与数字文件一致）

- 对话内直写 + 参数指纹幂等 + 本地 outbox 对账状态
- Redis 用户级并发/配额限流；工具级超时；写熔断
- 离线黄金集产出工具选择准确率与任务完成率（见 `target/golden-metrics.json`）
- 检索片段与工具返回不可信隔离；审计落 PG

## 仍不要写

- MCP、Elasticsearch 混合检索、RAGAS、完整 OTel 平台（未做）
- 「确认卡防误触」（已废弃）

能力裁剪总表见 [personal-merchant-boundary.md](personal-merchant-boundary.md)。
