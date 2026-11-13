# booth-agent

挂在 `booth-cloud` 上的智能客服：查单、政策问答、退货 / 取消 / 改地址。

写操作在校验通过后直接提交商城网关（无确认卡），带写熔断与 outbox 幂等。Web 主路径使用 SSE：`POST /app/chat/stream`。

## Quick start

先启动 `booth-cloud` 网关（默认 `8080`）。

```bash
cd booth-agent
cp .env.example .env   # 按需修改
mvn -DskipTests package
docker compose up -d --build
```

- Agent: `http://localhost:8091`
- PG (pgvector): `5433`
- Redis: `6380`

网关地址默认 `host.docker.internal:8080`，可用环境变量覆盖。

本机只跑 jar：

```bash
powershell -File scripts\up-deps.ps1
java -jar target\booth-agent-1.0.0-SNAPSHOT.jar
```

## Behavior

| Endpoint | Notes |
|----------|-------|
| `POST /app/chat` | 同步对话（降级） |
| `POST /app/chat/stream` | SSE（Web 主路径） |
| `POST /app/chat/cancel` | 取消生成 |
| `GET /app/ops/summary` | 运营摘要（管理员 Token） |

工具白名单：`query_order`、`list_returns`、`prepare_return`、`prepare_cancel`、`prepare_change_address`、`transfer_human`、`search_policy`。

未配置模型密钥时规则选工具；配置 OpenAI 兼容密钥后可由模型选工具。政策知识库来自 `classpath:knowledge/*.md`。

Swagger 默认关闭：`BOOTH_SWAGGER_ENABLED=true`。

## Security

见 [SECURITY.md](SECURITY.md)。

## License

[MIT](LICENSE)
