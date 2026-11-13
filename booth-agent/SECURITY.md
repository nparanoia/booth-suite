# Security Policy（booth-agent）

面向本机演示的智能客服进程。详细已做项见 [`docs/security-basics.md`](docs/security-basics.md)；写操作口径见套件 [`docs/写操作约定.md`](../docs/写操作约定.md)。

## 生产前检查

| 项 | 说明 |
|----|------|
| 网关基址 | 只信任配置项 `booth.gateway-base-url`，演示登录禁止客户端自定义 gateway（防 SSRF） |
| 运营接口 | `/app/ops/**` 需管理员 Bearer；静态 `/ops.html` 不再公开拉数 |
| Swagger | **默认关闭**；本机联调设 `BOOTH_SWAGGER_ENABLED=true` 后访问 `/swagger-ui.html` |
| Actuator | 默认 `health,info`；扩大暴露用 `MANAGEMENT_ENDPOINTS_WEB_EXPOSURE_INCLUDE` |
| 数据库 | PG 口令与 `BOOTH_AGENT_DB_*` 勿用仓库示例值上公网 |
| LLM / 嵌入密钥 | 只放环境变量或本机 `.env`（不入库） |

## 威胁模型摘要

- Agent **不**替代商城鉴权：写操作透传会员令牌，由 booth-cloud 校验订单归属。  
- 知识库片段按不可信文本围栏处理；不能当作越权指令。  
- 写熔断 + 指纹/outbox 幂等减轻误触与重复单，**不能**抵御已窃取会员 Token 的合法身份滥用。

报告漏洞：勿在公开 Issue 贴密钥；联系 Maintainer。
