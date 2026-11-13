# Booth Suite

个人自营电商三件套（发布工作区）。

| 目录 | 说明 | 许可证 |
|------|------|--------|
| [`booth-cloud`](booth-cloud) | 微服务交易底座（网关 / 用户 / 商品 / 交易 / 搜索） | [MIT](booth-cloud/LICENSE) |
| [`booth-agent`](booth-agent) | 智能客服 Agent | [MIT](booth-agent/LICENSE) |
| [`booth-web`](booth-web) | Vue3 前台 + 店主后台 | [MIT](booth-web/LICENSE) |

- **品牌**：Booth · **Java 包**：`com.booth.*`
- **写操作约定**（三仓共用）：[`docs/写操作约定.md`](docs/写操作约定.md)
- **能力说明 / 指标样例**：[`docs/能力说明.md`](docs/能力说明.md) · [`docs/metrics/`](docs/metrics/)
- **发布扫缺 / 待办**：[`需求扫缺报告.md`](需求扫缺报告.md) · [`发布准备-todo.md`](发布准备-todo.md)

## 推荐启动顺序

1. **cloud 最小栈** → [`booth-cloud/docs/GETTING-STARTED.md`](booth-cloud/docs/GETTING-STARTED.md)（先 `gen-env` 再 compose）  
2. **web** → `cd booth-web && npm i && npm run dev`（仅商城）  
3. **agent**（要客服时）→ [`booth-agent/README.md`](booth-agent/README.md)  

含搜索：cloud compose 加 `--profile search`。  
演示账号：[`booth-cloud/docs/DEMO-DATA.md`](booth-cloud/docs/DEMO-DATA.md)。

## 安全

默认口令仅本地演示；**不要**把中间件端口暴露公网。策略与换密清单：[`SECURITY.md`](SECURITY.md)。  

**默认最小暴露**（关 API 文档 / prometheus）；`booth-cloud` 的 `gen-env` 会为本机打开 Mock + 文档便于自测。Agent Swagger 默认关，需要时设 `BOOTH_SWAGGER_ENABLED=true`。

支付能力对外口径：**Mock + 接口预留**（无真实支付宝沙箱密钥跑通）。

套件根目录 [LICENSE](LICENSE)（MIT）。
