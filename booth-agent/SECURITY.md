# Security（booth-agent）

智能客服服务。写操作在校验通过后提交商城网关，并具备写熔断与幂等。

## 部署检查

| 项 | 说明 |
|----|------|
| 网关基址 | 仅信任配置项 `booth.gateway-base-url`；演示登录禁止客户端自定义 gateway，降低 SSRF 风险 |
| 运营接口 | `/app/ops/**` 需要管理员 Bearer；静态页不直接拉取运营数据 |
| Swagger | 默认关闭；联调时可设 `BOOTH_SWAGGER_ENABLED=true` 访问 `/swagger-ui.html` |
| Actuator | 默认 `health,info`；扩大暴露使用 `MANAGEMENT_ENDPOINTS_WEB_EXPOSURE_INCLUDE` |
| 数据库 | `BOOTH_AGENT_DB_*` 使用独立强口令，不对公网开放 |
| LLM / 嵌入密钥 | 仅通过环境变量或本机 `.env` 注入，不入库 |

## 安全模型

- Agent 不替代商城鉴权：写操作透传会员令牌，由 booth-cloud 校验订单归属
- 知识库片段按不可信文本处理，不得作为越权指令来源
- 写熔断与指纹 / outbox 幂等用于抑制误触与重复提交；已泄露的会员 Token 仍可被合法身份滥用，需配合商城侧会话与权限策略
