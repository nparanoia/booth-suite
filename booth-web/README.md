# booth-web

Booth 前台商城 + 店主后台（Vue 3 + TypeScript + Vite）。

## Features

**Storefront `/`**

- 首页 / 分类 / 搜索 / 商品详情
- 购物车 / 结算 / 订单
- 登录注册 / 个人中心
- 智能客服（`booth-agent`）

**Admin `/admin`**

- 商品、订单、会员、售后
- 客服运营摘要（需管理员登录）

## Quick start

先启动 `booth-cloud`（网关 `8080`）。需要客服时再起 `booth-agent`（`8091`）。

```bash
cd booth-web
npm install
npm run dev
```

- Storefront: `http://localhost:5173/`
- Admin: `http://localhost:5173/admin`（`admin` / `admin123`，仅本地）

开发代理见 `vite.config.ts`：`/gw` → `8080`，`/agent` → `8091`。  
环境变量见 `.env.example`。生产反代示例：[`nginx.example.conf`](nginx.example.conf)。

## Security

Token 存 localStorage；生产请收紧 CSP / Cookie 策略。详见 [SECURITY.md](SECURITY.md)。

## License

[MIT](LICENSE)
