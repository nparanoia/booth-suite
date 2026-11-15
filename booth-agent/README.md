# booth-agent

挂载于 `booth-cloud` 的智能客服服务：支持查单、政策问答，以及退货 / 取消待付款 / 改地址等办理能力。

写操作经身份与订单校验后提交商城网关，具备写熔断与 outbox 幂等。Web 主路径使用 SSE：`POST /app/chat/stream`。

## 启动

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

网关地址默认 `host.docker.internal:8080`，可通过环境变量覆盖。

本机直接运行 jar：

```bash
powershell -File scripts\up-deps.ps1
java -jar target\booth-agent-1.0.0-SNAPSHOT.jar
```

## 接口

| Endpoint | 说明 |
|----------|------|
| `POST /app/chat` | 同步对话（降级） |
| `POST /app/chat/stream` | SSE（Web 主路径） |
| `POST /app/chat/cancel` | 取消生成 |
| `GET /app/ops/summary` | 运营摘要（管理员 Token） |

工具白名单：`query_order`、`list_returns`、`prepare_return`、`prepare_cancel`、`prepare_change_address`、`transfer_human`、`search_policy`。

未配置模型密钥时规则选工具；配置 OpenAI 兼容密钥后可由模型选工具。政策知识库来自 `classpath:knowledge/*.md`。

Swagger 默认关闭：`BOOTH_SWAGGER_ENABLED=true` 可开启。

## Security

详见 [SECURITY.md](SECURITY.md)。

## License

[MIT](LICENSE)
