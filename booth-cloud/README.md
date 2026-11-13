# booth-cloud

Booth 交易底座：Spring Cloud 网关 + 用户 / 商品 / 交易，可选搜索与观测。

## Modules

| Module | Port | Role |
|--------|------|------|
| booth-gateway | 8080 | API gateway |
| booth-user | 8081 | Members / admin / RBAC |
| booth-product | 8082 | Catalog / stock |
| booth-trade | 8083 | Orders / Mock pay / returns |
| booth-search | 8084 | Elasticsearch（profile `search`） |

## Quick start

```powershell
cd booth-cloud
powershell -File deploy\gen-env.ps1
mvn -DskipTests package
docker compose -f deploy/docker-compose.yml --env-file deploy/.env up -d --build
```

`gen-env` 会生成 `deploy/.env`（已 gitignore）。请确认 Nacos token 非空。

```text
# search
docker compose -f deploy/docker-compose.yml --env-file deploy/.env --profile search up -d

# observability
docker compose -f deploy/docker-compose.yml --env-file deploy/.env --profile obs up -d
```

默认库名：`booth_user` / `booth_product` / `booth_trade`。容器名 `booth-*`。

## Demo

- Admin: `admin` / `admin123`（仅本地）
- 商品种子：演示马克杯（product Flyway）
- 支付：Mock；需要时再配支付宝沙箱

客服写操作会回调本仓网关；订单归属与状态校验仍在本仓完成。

## Security

见 [SECURITY.md](SECURITY.md)。默认关闭 API 文档与多余 Actuator；`gen-env` 可为本机打开 Mock / 文档。

## License

[MIT](LICENSE)
