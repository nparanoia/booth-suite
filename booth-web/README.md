# Booth booth-web

对接 `booth-cloud` 的 Vue3 + TypeScript **个人自营小店**：**顾客前台 + 店主后台** 集成在同一工程。

观感参考小米商城（橙黑白）；信息架构参考 Shopper / Tigshop。

## 客服写操作

Agent 校验后直接提交商城，本仓不画确认卡。聊天走 `/agent` → `/app/chat/stream`。口径见 [docs/写操作约定.md](../docs/写操作约定.md)。

## 功能

### 用户前台 `/`

- 首页 / 分类 / 搜索 / 商品详情
- 购物车 / 结算 / 订单列表与详情（支付、取消、确认收货）
- 登录注册 / 个人中心
- 智能客服（对接 `booth-agent`）
- PC + 手机响应式

### 管理后台 `/admin`

- 登录（演示：`admin` / `admin123`）
- 可视化总览（商品/订单/会员/售后指标 + 柱状图）
- 商品上下架、删除
- 订单发货、关闭待付款
- 会员启用/禁用
- 售后同意/拒绝/确认收货
- 客服运营面板（对接 booth-agent `/app/ops/summary`，携带管理员 Token）

没有后端能力的入口做成禁用按钮，不假装能用。

基础安全：运营接口鉴权、商品详情 HTML 净化、CSP / 安全响应头、管理登录不预填密码。诚实风险（localStorage Token、CSP 生产应收口等）见 [`SECURITY.md`](SECURITY.md)；Agent 侧见 `booth-agent/docs/security-basics.md`。

## 生态链接

- 交易底座：[`booth-cloud`](../booth-cloud) · [开箱](../booth-cloud/docs/GETTING-STARTED.md) · [演示数据](../booth-cloud/docs/DEMO-DATA.md)  
- 智能客服：[`booth-agent`](../booth-agent)  
- 套件：[`../README.md`](../README.md)  
- 许可证：[LICENSE](LICENSE)（MIT）

## 本地运行（两档）

### A. 最小商城（只要逛店下单）

1. 按 cloud [GETTING-STARTED](../booth-cloud/docs/GETTING-STARTED.md) 起**最小栈**（网关 8080）  
2. 前端：

```bash
cd booth-web
cp .env.example .env.development   # 若尚无
npm install
npm run dev
```

- 商城：`http://localhost:5173/`  
- 后台：`http://localhost:5173/admin`（`admin` / `admin123`，仅本地）

### B. 完整客服

在 A 基础上再起 `booth-agent`（8091）。客服走 SSE：`/agent/app/chat/stream`。

开发态 Vite 代理：`/gw` → `8080`，`/agent` → `8091`。  
生产反代示例：[`docs/nginx.example.conf`](docs/nginx.example.conf)。  
环境变量见 [`.env.example`](.env.example)；品牌名 `VITE_BRAND` 默认 **Booth**。
