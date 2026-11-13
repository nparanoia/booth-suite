# Booth Suite

个人自营小店演示：下单、支付（Mock）、售后，再挂一个能查单/改地址/退货的客服。

| 目录 | 做什么 |
|------|--------|
| [booth-cloud](booth-cloud) | 网关 + 用户/商品/交易（可选搜索） |
| [booth-agent](booth-agent) | 智能客服 |
| [booth-web](booth-web) | 前台商城和店主后台 |

许可证 MIT。包名 `com.booth.*`。

## 怎么跑

先起商城，再起前端；要客服时再起 agent。

1. Cloud：照 [booth-cloud/docs/GETTING-STARTED.md](booth-cloud/docs/GETTING-STARTED.md)（先 `gen-env`，再 compose）
2. Web：`cd booth-web && npm i && npm run dev`
3. Agent：见 [booth-agent/README.md](booth-agent/README.md)

搜索：compose 加 `--profile search`。  
演示账号：[DEMO-DATA.md](booth-cloud/docs/DEMO-DATA.md)。

截图在 [docs/demo/screenshots](docs/demo/screenshots)。客服写操作怎么约定的：[docs/写操作约定.md](docs/写操作约定.md)。

## 注意

默认账号密码只适合本机玩，别把中间件端口暴露到公网。细节见 [SECURITY.md](SECURITY.md)。

支付对外口径是 **Mock + 接口预留**，没配真沙箱密钥就别说已跑通支付宝。
