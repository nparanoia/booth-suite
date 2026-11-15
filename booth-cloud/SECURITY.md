# Security（booth-cloud）

默认配置面向本机 Docker 联调。将 3306 / 6379 / 15672 / 8090 / 9090 / 9411 等端口映射到公网属于高风险暴露。

## 生产换密清单

| 项 | 本地默认 | 生产要求 |
|----|----------|----------|
| `BOOTH_SECURITY_SECRET` | `dev-only-secret-change-me-…`（见 `booth-security.yml`） | ≥32 字节随机；依赖该密钥的服务保持一致 |
| MySQL `MYSQL_ROOT_PASSWORD` / 业务库账号 | `root` / `booth` + `booth123` | 强口令；业务账号最小权限 |
| Redis | 无密码 | `requirepass`，仅内网 |
| RabbitMQ | `booth` / `booth123` | 强口令；管理台不对公网开放 |
| Nacos `NACOS_AUTH_TOKEN` 等 | 由 `gen-env` 生成；空 token 禁止 | 保持非空；控制台不对公网开放 |
| Elasticsearch | 无认证（search profile） | 开启安全或仅内网 |
| 店主 `admin` / `admin123` | 演示账号 | 首次登录强制改密；禁用演示账号 |
| Grafana | `admin` / `admin`；可关匿名 Viewer | 关闭匿名访问；改密；obs 仅内网 |
| Mock 支付 | 见下文 | 关闭 mock 与 `ALLOW_DEV_SECRET` |

生成示例：

```bash
openssl rand -base64 32   # BOOTH_SECURITY_SECRET / Nacos token 等
```

## 默认开关

仓库代码默认偏收紧；本机执行 `gen-env` 会打开部分演示开关以便联调。

| 开关 | 代码 / compose 默认 | `gen-env` / `start-apps.ps1` 本机 |
|------|---------------------|----------------------------------|
| Mock 支付 + `ALLOW_DEV_SECRET` | `false` | `true` |
| `API_DOCS_ENABLED`（Knife4j / springdoc） | `false` | `true` |
| Agent `BOOTH_SWAGGER_ENABLED` | `false` | 需在 agent `.env` 自行开启 |
| Actuator `MANAGEMENT_ENDPOINTS_WEB_EXPOSURE_INCLUDE` | `health,info` | 同左（需要时可再加 prometheus） |

`booth-trade` 在 mock 开启、密钥仍为仓库默认且未设置 `ALLOW_DEV_SECRET` 时会拒绝启动。

## 信任边界

- 浏览器 / 公网流量应仅到达 **gateway:8080**（或经反向代理后的 web / agent）。
- 业务服务 8081–8084 与中间件端口假定部署在私有网络；compose 映射到宿主机仅用于本机开发。

### 服务间接口

部分服务提供 `/inner/**` 供内部调用。当前依赖网络隔离与共享密钥约定，生产环境应：

- 不对外暴露业务端口
- 轮换 `BOOTH_SECURITY_SECRET`
- 禁止经网关外路径直达 inner 接口

### 其他说明

- 前端登录态使用 `localStorage`（见 booth-web `SECURITY.md`）
- 登录失败按 IP 限流（约 15 分钟 10 次），需配合强口令使用
- 支付为 Mock，并预留扩展接口

## 生产收口

1. 反向代理仅放行 gateway（及静态 web）；业务与中间件监听内网，勿映射 8081–8084 / 3306 等
2. 保持 `API_DOCS_ENABLED=false`、`BOOTH_SWAGGER_ENABLED=false`；Actuator 仅暴露 `health`（或内网鉴权后开放 prometheus）
3. 关闭 Grafana 匿名访问；Nacos / Rabbit 管理台不对公网开放
4. 全站 HTTPS；密钥通过环境变量注入；发布前做密钥扫描

启动步骤见 [README.md](README.md)。
