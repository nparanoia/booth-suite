# 波次 4 验收报告（2026-11-14）

环境：本机 `booth-cloud` + `booth-agent` compose（非空卷从零；库为新 `booth_*`）。明细日志：`wave4-acceptance.txt`。

## 结论

**宣称清单（API）整体 PASS。** 发现若干**演示站可接受、生产必须收口**的暴露面，以及 1 个联调配置坑（已修）。

## 宣称清单

| 项 | 结果 |
|----|------|
| 开箱健康 gateway/agent | PASS |
| 注册/登录/admin | PASS |
| 下单 → Mock 支付 → 发货 → 确认收货 | PASS |
| 政策问答 + sources | PASS |
| SSE progress+done | PASS |
| 查己单 | PASS（修 gateway URL 后） |
| 越权查单（Agent + 交易 API） | PASS |
| 退货直写 + 幂等 | PASS（售后单 1，二次「未重复建单」） |
| 取消待付款 | PASS |
| confirm 废弃提示 | PASS |
| ops 匿名 401 / 管理员可看 | PASS |
| 登录限流 11 次 → 10006 | PASS |
| 转人工 | PASS |
| 知识拒答（海外仓） | PASS |
| 改地址话术 | **WEAK**：被理解成查单，未触发改地址工具（规则分流/话术敏感） |
| Web :5173 | 本轮未稳定拉起（API 已覆盖；浏览器点验可另补） |

## 安全 / 边界发现

| 级别 | 发现 | 建议 |
|------|------|------|
| 高（联调） | 宿主机若设 `BOOTH_GATEWAY_BASE_URL=http://127.0.0.1:8080`，会污染 agent compose，容器内打到自己 →「商城暂时不可用」 | 已改为 `AGENT_GATEWAY_URL` / 默认 `host.docker.internal` |
| 中（已收口） | Agent Swagger / 网关 doc / prometheus | 代码默认关；`gen-env` 仅开文档便于本机测；Actuator 默认 `health,info` |
| 低 | `/trade/inner` 经网关 403；直连业务端口亦 403（本环境） | 仍勿暴露 8081–8084 |
| 低 | Mock 坏签名 → `failure` | OK |
| 低 | CORS 登录 OPTIONS 未对 evil 放开 | OK |
| 信息 | 会员/管理员 Token 隔离、聊天未登录 401 | OK |

## 公开前卫生

- `SECURITY.md` / README「仅本地」：有  
- 密钥扫描报告：`secret-scan-20261114.txt`  

## 未阻断项（诚实边界）

真支付宝沙箱、httpOnly Cookie、MCP/多 Agent、完整 OTel、改地址话术鲁棒性增强、Web 浏览器点验本轮未强制。
