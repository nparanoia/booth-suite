# booth-cloud

自营电商交易底座：基于 Spring Cloud 提供 API 网关、用户、商品与交易服务，并支持可选的搜索与观测组件。

## 模块

| 模块 | 端口 | 职责 |
|------|------|------|
| booth-gateway | 8080 | API 网关 |
| booth-user | 8081 | 会员 / 管理员 / RBAC |
| booth-product | 8082 | 商品与库存 |
| booth-trade | 8083 | 订单 / Mock 支付 / 售后 |
| booth-search | 8084 | Elasticsearch（profile `search`） |

## 启动

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

## 说明

- 管理员：`admin` / `admin123`（仅本地）
- 商品种子：演示马克杯（product Flyway）
- 支付：Mock，并预留扩展接口
- 客服写操作回调本仓网关；订单归属与状态校验由本仓完成

## Security

详见 [SECURITY.md](SECURITY.md)。默认关闭 API 文档与多余 Actuator；`gen-env` 可为本机打开 Mock / 文档。

## License

[MIT](LICENSE)
