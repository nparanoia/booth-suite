# booth-cloud 开箱指南

> **目标态**：全新机器按本文用 `booth_*` 库名与 `booth-*` 容器启动。  
## 前提

- JDK 17、Maven 3.9+、Docker Desktop（含 Compose）
- 首次需要能访问 Docker Hub；RabbitMQ 延迟插件镜像与 ES+IK 为本地 build

## 1. 生成环境变量（禁止空 Nacos token）

```powershell
cd booth-cloud
powershell -File deploy\gen-env.ps1
```

或：`bash deploy/gen-env.sh`  
会生成 `deploy/.env`（已 gitignore）。打开确认 `NACOS_AUTH_TOKEN`、`NACOS_AUTH_IDENTITY_VALUE` 非空。

## 2. 打包业务 jar

在 `booth-cloud` 根目录：

```bash
mvn -DskipTests package
```

最小栈需要：`booth-user` / `booth-product` / `booth-trade` / `booth-gateway`。  
含搜索时再确保 `booth-search` 也打出 jar。

## 3. 构建自定义镜像并启动

```powershell
cd booth-cloud
# 首次或 Dockerfile 变更后
docker compose -f deploy/docker-compose.yml --env-file deploy/.env build rabbitmq

# 最小栈（交易闭环；不含 ES / search / 监控）
docker compose -f deploy/docker-compose.yml --env-file deploy/.env up -d

# 含搜索（需先有 IK；见下节）
docker compose -f deploy/docker-compose.yml --env-file deploy/.env --profile search build elasticsearch
docker compose -f deploy/docker-compose.yml --env-file deploy/.env --profile search up -d

# 监控（Grafana/Prometheus/Zipkin）
docker compose -f deploy/docker-compose.yml --env-file deploy/.env --profile obs up -d

# 全开
docker compose -f deploy/docker-compose.yml --env-file deploy/.env --profile full up -d
```

业务容器挂载 `*/target/*.jar`；改代码后重新 `mvn package` 再 `docker compose restart booth-user`（或对应服务）。

本机起 jar（与 Docker 业务容器二选一，勿抢端口）：

```powershell
powershell -File deploy\start-apps.ps1
```

## 4. Elasticsearch IK（仅 search / full）

`deploy/elasticsearch/Dockerfile` 会安装与 `ES_VERSION` 一致的 IK。

- **在线**：`compose build elasticsearch` 时从 Infini 拉取。  
- **离线**：下载 `elasticsearch-analysis-ik-${ES_VERSION}.zip` 放到 `deploy/elasticsearch/plugins/` 后再 build（该目录 zip 已 gitignore）。

## 5. 端口矩阵

| 端口 | 服务 |
|------|------|
| 8080 | 网关 |
| 8081 | user |
| 8082 | product |
| 8083 | trade |
| 8084 | search（profile search/full） |
| 3306 | MySQL |
| 6379 | Redis |
| 5672 / 15672 | RabbitMQ |
| 8848 / 9848 / 8090 | Nacos（控制台映射 8090） |
| 9200 | ES（search/full） |
| 8858 | Sentinel |
| 9090 / 3000 / 9411 | Prometheus / Grafana / Zipkin（obs/full） |

## 6. 安全声明（仅本地演示）

- 默认口令见 `.env.example` / README（`root` / `booth123` / `admin123` 等）**禁止映射到公网**。  
- **仓库默认**：Mock 支付关、API 文档关、Actuator 仅 `health,info`。  
- **`gen-env` / `start-apps.ps1`**：会打开 Mock + API 文档，方便本机/面试官自测；Agent Swagger 需在 agent `.env` 设 `BOOTH_SWAGGER_ENABLED=true`。  
- 细节见 **[`../SECURITY.md`](../SECURITY.md)**。

## 7. 排障

| 现象 | 处理 |
|------|------|
| Nacos 起不来 / 客户端连不上 | 检查 `deploy/.env` token 是否为空；重新 `gen-env.ps1 -Force` |
| `pull_policy` / 自定义镜像找不到 | `compose build rabbitmq`（及 elasticsearch） |
| 业务 jar 缺失 | 先 `mvn -DskipTests package` |
| 搜索 404 / 无结果 | 是否加了 `--profile search`；Canal + ES 是否 healthy；商品是否已上架并同步 |
| 端口占用 | 停掉本机其它占用 8080–8084 / 3306 / 8848 的进程或旧容器 |

## 8. 下一步

- 演示账号与样例订单：[`DEMO-DATA.md`](DEMO-DATA.md)  
- 前台：`booth-web`；客服：`booth-agent`  
- 套件总览：仓库根 `README.md`
