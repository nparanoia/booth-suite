# Security（booth-web）

会员端与店主后台（Vue 3）。默认经本机 Vite 或 nginx 反向代理访问 gateway / agent。

## 会话与前端安全

| 项 | 当前实现 | 生产建议 |
|----|----------|----------|
| 登录态 | 会员 / 管理员 Token 存放于 `localStorage` | 可升级为 httpOnly Cookie，并配合 CSRF 防护（需后端支持） |
| 客服会话 id | `ql_support_cid` 存放于 `localStorage`；logout 时清理 | 避免多账号共用未退出的浏览器会话 |
| CSP | `index.html` meta 含 `frame-ancestors 'none'` 等；Vite dev / preview 另有 `X-Frame-Options` | 生产由 nginx 响应头下发完整 CSP |
| 富文本 | 商品详情经 DOMPurify 后 `v-html` | 保持净化；CMS 内容仍需服务端消毒 |
| 支付回跳 | 可能涉及网关返回的 HTML | 仅信任本站网关；全站 HTTPS |

## nginx

反代 `/gw`、`/agent` 示例见 README。生产建议叠加：

- `Content-Security-Policy`（按静态资源域名与 API 收紧 `connect-src`）
- `Strict-Transport-Security`（仅 HTTPS）
- 不对外暴露源站 Actuator / Swagger 路径

## 管理员账号

演示账号：`admin` / `admin123`（仅本地）。服务端对登录失败实施 IP 限流，部署前请修改默认口令。
