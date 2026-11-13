# Security Policy（booth-web）

顾客前台与店主后台（Vue3）。默认假设经本机 Vite 或 nginx 反代访问 gateway / agent。

## 已知演示站风险（诚实）

| 项 | 现状 | 生产建议 |
|----|------|----------|
| 登录态 | 会员/管理员 Token 在 `localStorage` | httpOnly Cookie + CSRF 策略（需后端配合；演示站明确不做） |
| 客服会话 id | `ql_support_cid` 在 `localStorage`；logout 会清理 | 同左；勿跨账号共用浏览器未退出会话 |
| CSP | `index.html` meta 含 `frame-ancestors 'none'` 等；Vite dev/preview 另有 `X-Frame-Options` | 生产由 **nginx 响应头** 下发完整 CSP，勿只依赖 meta |
| 富文本 | 商品详情经 DOMPurify 再 `v-html` | 保持净化；CMS 来源仍需服务端消毒 |
| 支付回跳页 | 可能涉及网关返回的 HTML | 仅信任本站网关；全站 HTTPS |

## nginx 提示

反代 `/gw`、`/agent` 示例见 README。生产请叠加：

- `Content-Security-Policy`（按实际静态域名与 API 收紧 `connect-src`）  
- `Strict-Transport-Security`（仅 HTTPS）  
- 禁止把源站 Actuator / Swagger 路径暴露到公网

## 店主口令

演示账号见 booth-cloud [`docs/DEMO-DATA.md`](../booth-cloud/docs/DEMO-DATA.md)（常见 `admin` / `admin123`）。登录失败有服务端 IP 限流；**不能**代替改密。

报告漏洞：勿公开贴 Token；联系 Maintainer。
