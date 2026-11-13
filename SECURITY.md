# Security

本仓库是**本地演示**套件，不是生产加固模板。

| Component | Notes |
|-----------|--------|
| booth-cloud | [booth-cloud/SECURITY.md](booth-cloud/SECURITY.md) |
| booth-agent | [booth-agent/SECURITY.md](booth-agent/SECURITY.md) |
| booth-web | [booth-web/SECURITY.md](booth-web/SECURITY.md) |

建议：

1. 部署前修改 `admin` 等默认口令
2. 不要把 MySQL / Redis / Nacos / RabbitMQ 等端口暴露公网
3. 生产关闭 Mock 支付、Swagger / API 文档、匿名观测面板
4. 客服写操作依赖商城侧身份与订单归属校验；Agent 另有写熔断与幂等
