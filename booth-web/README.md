# booth-web

会员端商城与店主后台，基于 Vue 3、TypeScript 与 Vite。

## 功能

**会员端 `/`**

- 首页 / 分类 / 搜索 / 商品详情
- 购物车 / 结算 / 订单
- 登录注册 / 个人中心
- 智能客服（`booth-agent`）

**店主后台 `/admin`**

- 商品、订单、会员、售后
- 客服运营摘要（需管理员登录）

## 启动

先启动 `booth-cloud`（网关 `8080`）。需要客服时再启动 `booth-agent`（`8091`）。

```bash
cd booth-web
npm install
npm run dev
```

- 会员端：`http://localhost:5173/`
- 店主后台：`http://localhost:5173/admin`（`admin` / `admin123`，仅本地）

开发代理见 `vite.config.ts`：`/gw` → `8080`，`/agent` → `8091`。  
环境变量见 `.env.example`。生产反代示例：[`nginx.example.conf`](nginx.example.conf)。

## Security

详见 [SECURITY.md](SECURITY.md)。生产环境请收紧 CSP，并按需升级 Cookie 会话策略。

## License

[MIT](LICENSE)
