# booth-agent 开发计划（归档摘要）

独立服务，对接 `booth-cloud`。能力范围以 [personal-merchant-boundary.md](personal-merchant-boundary.md) 为准。

阶段 1–5 与扩展 1–7 的流水账文档已从本仓库移除（发布清理）；实现与验收以源码、测试与边界/安全文档为准。

## 现状摘要

- 规则意图分流 + 可选模型选工具；工具白名单；步数/超时/错误分类；模型失败降级规则路径。
- 知识库：Markdown 切块 → PostgreSQL + pgvector；阈值拒答；管理端补库与时效。
- 写操作：对话内校验后直写商城（退货/取消/改地址）；写熔断；幂等/outbox 按发布扫缺报告补挂。
- 会话：近 10 轮 + 摘要；入口带单；SSE 流式；轨迹打码；GoldenSet；运营摘要与对话取证。

## 明确不做

见 [personal-merchant-boundary.md](personal-merchant-boundary.md) 省略包。

相关：security-basics.md、p0-hardening.md。