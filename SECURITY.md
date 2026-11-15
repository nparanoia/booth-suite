# Security

本仓库默认配置面向本机联调与演示。对外部署前请完成换密与网络收口。

| 组件 | 说明 |
|------|------|
| booth-cloud | [booth-cloud/SECURITY.md](booth-cloud/SECURITY.md) |
| booth-agent | [booth-agent/SECURITY.md](booth-agent/SECURITY.md) |
| booth-web | [booth-web/SECURITY.md](booth-web/SECURITY.md) |

部署建议：

1. 修改 `admin` 等默认口令
2. 数据库、Redis、Nacos、RabbitMQ 等中间件仅内网可达
3. 关闭 Mock 支付、Swagger / API 文档，以及未鉴权的观测面板
4. 客服写操作由商城侧完成身份与订单归属校验；Agent 侧另有写熔断与幂等
