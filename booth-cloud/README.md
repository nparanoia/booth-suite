# booth-cloud

Booth 微服务交易底座：**网关 / 用户 / 商品 / 交易 / 搜索**。

| 兄弟项目 | 说明 |
|----------|------|
| [`booth-web`](../booth-web) | Vue3 顾客前台 + 店主后台 |
| [`booth-agent`](../booth-agent) | 智能客服（对接本仓网关） |
| 套件根目录 | [`../README.md`](../README.md) · [`../docs/写操作约定.md`](../docs/写操作约定.md) |

许可证：[LICENSE](LICENSE)（MIT）

## 写操作约定（三仓共用）

全文见 [`docs/写操作约定.md`](../docs/写操作约定.md)（与 agent / web **一字不差**）。

摘要：客服写操作 = **校验后对话内直写本仓网关** + Agent 侧写熔断与指纹/outbox 幂等；本仓继续做身份与订单归属校验。无确认卡。

## 5 分钟路径（最小交易栈）

```powershell
cd booth-cloud
powershell -File deploy\gen-env.ps1
mvn -DskipTests package
docker compose -f deploy/docker-compose.yml --env-file deploy/.env build rabbitmq
docker compose -f deploy/docker-compose.yml --env-file deploy/.env up -d
```

网关：`http://localhost:8080`  
详细步骤、profile（search / obs / full）、IK、排障 → **[`docs/GETTING-STARTED.md`](docs/GETTING-STARTED.md)**

演示账号与种子说明 → [`docs/DEMO-DATA.md`](docs/DEMO-DATA.md)

## 模块与默认端口

| 模块 | 端口 | 说明 |
|------|------|------|
| booth-gateway | 8080 | 统一入口 |
| booth-user | 8081 | 会员 / 管理员 / RBAC |
| booth-product | 8082 | 商品 / 库存 |
| booth-trade | 8083 | 订单 / 支付(Mock) / 售后 |
| booth-search | 8084 | ES 检索（compose profile `search`/`full`） |

## Compose 档位

| 命令 | 内容 |
|------|------|
| 默认 `up -d` | MySQL / Redis / RabbitMQ / Nacos / Sentinel / Canal + user/product/trade/gateway |
| `--profile search` | + Elasticsearch(IK) + booth-search |
| `--profile obs` | + Prometheus / Grafana / Zipkin |
| `--profile full` | search + obs |

**最小栈不含搜索**：前台搜索依赖 8084 时请开 `search`，或在材料中写明「最小栈无搜索」。

## 安全（仅本地）

默认密钥与弱口令仅供本机演示；**不要**把 3306 / 6379 / 15672 / 8090 / 9090 等暴露到公网。  
默认关 Knife4j/springdoc 与多余 Actuator；`gen-env` 会为本机打开 Mock + API 文档。  
换密清单 / 威胁模型 → **[`SECURITY.md`](SECURITY.md)**。

## 数据与命名

Compose 默认库：`booth_user` / `booth_product` / `booth_trade`；容器名 `booth-*`。勿与历史工程数据卷混用。
