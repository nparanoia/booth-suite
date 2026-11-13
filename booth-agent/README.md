# booth-agent

挂在 `booth-cloud` 上的电商智能客服。查单和售后走受控工具，退换货政策走知识库。

## 写操作

退货 / 取消 / 改地址：校验通过后直接打商城网关，对话里出结果（没有确认卡）。带写熔断和指纹/outbox 幂等。Web 主路径走 SSE：`/app/chat/stream`。

约定全文：[docs/写操作约定.md](../docs/写操作约定.md)。

没做也不宣称的：MCP、多 Agent、完整 OTel、呼叫中心、真支付宝沙箱跑通等。

## 生态链接

- 交易底座：[`booth-cloud`](../booth-cloud) · [开箱](../booth-cloud/docs/GETTING-STARTED.md)  
- 前台 / 后台：[`booth-web`](../booth-web)  
- 套件：[`../README.md`](../README.md) · 写操作约定见上文  
- 许可证：[LICENSE](LICENSE)（MIT）

## 和现有工程的关系

- **独立仓库式目录**，不塞进 `booth-cloud` 模块里。
- 业务对接 `booth-cloud` 网关；没配密钥时用规则选工具；配了 OpenAI 兼容密钥后由模型选工具。政策文档切块后写入本机 PostgreSQL + pgvector。
- **个人自营裁剪边界**：见 [`docs/personal-merchant-boundary.md`](docs/personal-merchant-boundary.md)。


## 本地运行

1. 先启动 `booth-cloud`（`deploy/docker-compose.yml` 已默认含中间件 + user/product/trade/gateway；需先 `mvn -DskipTests package`）。
2. 复制 `.env.example` 里的变量到环境，或改 `application.yml`。
3. **推荐：Docker 开箱起 PG + Redis + Agent**（需先 package）：

```text
mvn -DskipTests package
docker compose up -d --build
```

PostgreSQL+pgvector 映射 `5433`，独立 Redis 映射 `6380`，Agent `:8091`（网关默认 `host.docker.internal:8080`）。仓库根目录也可用 `powershell -File scripts\dev-docker.ps1`（两租户后端 + 本终端 Vue）。

仅起中间件、本机跑 jar：`powershell -File scripts\up-deps.ps1`，再：

```bash
set JAVA_HOME=D:\JAVA\jdk17
java -jar target\booth-agent-1.0.0-SNAPSHOT.jar
```

也可不用 Docker、仅起嵌入式知识库进程：`powershell -File scripts\start-knowledge-db.ps1`。

默认端口 `8091`。演示页：`http://localhost:8091/`。运营数据请到 `booth-web` 管理后台 `/admin/ops`（需管理员登录）。Swagger **默认关闭**；本机设 `BOOTH_SWAGGER_ENABLED=true` 后访问 `/swagger-ui.html`（见 [`SECURITY.md`](SECURITY.md)）。

## 当前范围

- `POST /app/chat`：白名单工具，一轮最多 4 步。没配模型密钥时规则选工具；配了密钥则模型选工具，订单和政策仍以工具结果为准。
- `POST /app/chat/stream`：SSE（Web 主路径）。先推进度（正在查订单 / 正在检索 / 正在提交退货等），再推正文。`POST /app/chat/cancel` 用 `runId` 停下后续生成。
- `POST /app/chat/confirm`：**已废弃**（仍返回提示，不写商城）。写操作在对话内直办。
- 工具：`query_order`、`list_returns`、`prepare_return`、`prepare_cancel`、`prepare_change_address`、`transfer_human`、`search_policy`。
- 写操作：校验通过后直写商城；经参数指纹 + `write_action_outbox` 幂等；`writeCircuitOpen` 写熔断仍生效。
- Embedding：默认本地 n-gram；配 `OPENAI_API_KEY` 且 `booth.agent.embedding.provider=auto|openai` 时用 `text-embedding-3-small`（256 维）。
- 没有权限或被商城拒绝时按错误分类处理；读超时最多再试 2 次。
- 知识库：`classpath:knowledge/*.md` 按标题切块，向量召回，短语命中作为补充。低于阈值拒答，并标来源。片段不能当成退货指令。
- 会话：同一个 `conversationId` 保留最近 10 轮原文，更早的压成摘要。下一句「就退这个」沿用上一轮订单号。
- 轨迹：`GET /app/chat/traces?conversationId=` 能看到这一轮用了哪个工具。参数里的令牌和手机号会打码。黄金用例是 `logs/agent-golden.ps1`。
- 运营：`GET /app/ops/summary` 需管理员 Token；字段为 `writeReturns` / `writeCancels` / `writeFails`；见 `docs/security-basics.md`。
- 真实商城联调：`logs/live-accept.ps1`（需网关和 Agent 都在听）。
