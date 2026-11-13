# Security Policy（booth-cloud）

本仓库默认配置面向 **本机 Docker 演示**，不是生产基线。把 3306 / 6379 / 15672 / 8090 / 9090 / 9411 等映射到公网即视为主动暴露。

## 报告漏洞

请勿在公开 Issue 贴密钥或生产数据。可通过仓库 Maintainer 私信或邮件说明复现步骤；无专项赏金。

## 生产换密清单（上线前必须）

| 项 | 本地默认（勿上公网） | 生产要求 |
|----|----------------------|----------|
| `BOOTH_SECURITY_SECRET` | `dev-only-secret-change-me-…`（见 `booth-security.yml`） | ≥32 字节随机；所有依赖该密钥的服务一致 |
| MySQL `MYSQL_ROOT_PASSWORD` / 业务库账号 | `root` / `booth`+`booth123` | 强口令；业务账号最小权限 |
| Redis | 无密码 | `requirepass` + 仅内网 |
| RabbitMQ | `booth` / `booth123` | 强口令；管理台勿对公网 |
| Nacos `NACOS_AUTH_TOKEN` 等 | 由 `gen-env` 生成；空 token 禁止 | 保持非空；控制台勿裸奔公网 |
| Elasticsearch | 无认证（search profile） | 开启安全或仅内网 |
| 店主 `admin` / `admin123` | DEMO-DATA 演示号 | 首次登录强制改密；关闭演示账号 |
| Grafana | `admin`/`admin`；匿名 Viewer 可关 | 关匿名；改密；obs 仅内网 |
| Mock 支付 | 见下文 | **关闭** mock 与 `ALLOW_DEV_SECRET` |

生成示例：

```bash
openssl rand -base64 32   # BOOTH_SECURITY_SECRET / Nacos token 等
```

## Compose 默认暴露面（面试作品基线）

仓库默认偏安全；**克隆本机用 `gen-env` 会打开演示开关**，方便自测。

| 开关 | 代码/compose 默认 | `gen-env` / `start-apps.ps1` 本机演示 |
|------|-------------------|--------------------------------------|
| Mock 支付 + `ALLOW_DEV_SECRET` | `false` | `true` |
| `API_DOCS_ENABLED`（Knife4j / springdoc） | `false` | `true` |
| Agent `BOOTH_SWAGGER_ENABLED` | `false` | 需在 agent `.env` 自行 `true` |
| Actuator `MANAGEMENT_ENDPOINTS_WEB_EXPOSURE_INCLUDE` | `health,info` | 同左（要 prometheus 再加） |

应用层：`booth-trade` 在 mock 开启且密钥仍是仓库默认、又未 `ALLOW_DEV_SECRET` 时会拒绝启动。

## 威胁模型（诚实边界）

### 信任边界

- **浏览器 / 公网** → 仅应到达 **gateway:8080**（及反向代理后的 web/agent）。  
- **业务服务 8081–8084、中间件端口**：假定在私有网络；compose 映射到宿主机只为本地开发。

### inner 接口

部分服务暴露 `/inner/**` 供服务间调用。当前防护主要是 **网络隔离 + 共享密钥/约定**，不是完整 mTLS / 防重放。

**若攻击者能直连业务端口，并掌握（或猜中）仓库默认 `BOOTH_SECURITY_SECRET`，即可伪造 inner 调用。**  
生产必须：不暴露业务端口、轮换密钥、网关外禁止直达 inner。

### 已知演示站风险

- 会员/管理员 Token 在前端 `localStorage`（见 booth-web `SECURITY.md`）。  
- 登录失败按 IP 限流（约 15 分钟 10 次），降低撞 `admin123`，不能替代强口令。  
- Token 在前端 `localStorage`（见 booth-web）。  
- 支付宝为 **Mock + 接口预留**，无真实沙箱密钥跑通。

## 建议的生产收口

1. 反向代理只放行 gateway（及静态 web）；业务与中间件 listen 内网；勿映射 8081–8084/3306 等。  
2. 保持 `API_DOCS_ENABLED=false`、`BOOTH_SWAGGER_ENABLED=false`；Actuator 仅 `health`（或内网鉴权后开 prometheus）。  
3. 关 Grafana 匿名、Nacos/Rabbit 管理台裸奔。  
4. HTTPS；密钥走环境变量；发布前密钥扫描（P1-S7）。

相关：[`docs/GETTING-STARTED.md`](docs/GETTING-STARTED.md) · [`docs/DEMO-DATA.md`](docs/DEMO-DATA.md) · 套件 [`发布准备-todo.md`](../发布准备-todo.md)
