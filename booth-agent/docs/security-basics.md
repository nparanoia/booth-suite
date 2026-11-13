# 基础安全（演示站也要有）

日期：2026-10-30。针对「明显演示级缺口」做的最小补强，方便面试讲清楚边界。

## 已补

1. **运营接口鉴权**  
   `GET /app/ops/summary` 必须带管理员 `Authorization: Bearer`。Agent 向网关 `GET /user/admin/auth/me` 校验。匿名访问返回 401/403。

2. **静态 ops 页**  
   `/ops.html` 不再拉公开接口，改为引导去 `booth-web` 管理后台。

3. **演示登录防 SSRF**  
   `/app/demo/login` 不再接受客户端自定义 `gateway`，只打配置里的 `booth.gateway-base-url`。同一 IP 一分钟失败过多返回 429。

4. **安全响应头**  
   Agent 增加 `X-Content-Type-Options`、`X-Frame-Options`、`Referrer-Policy`、基础 CSP 等。

5. **前台**  
   - 商品详情 HTML 经 DOMPurify 净化后再 `v-html`  
   - 管理登录不预填密码  
   - 运营面板请求携带管理员 Token  
   - `index.html` 增加 CSP / referrer meta；Vite 开发服加基础安全头  

6. **Agent 检索 / 写操作隔离（P1）**  
   - `UntrustedText`：知识片段、工具回传、会话摘要加围栏并打散常见注入前缀  
   - 写操作：对话内直写 + 会员令牌透传商城校验归属；参数指纹 + outbox 幂等；写熔断  
   - GoldenSet 覆盖「越权查单」；写操作审计落 PG（成功不被后续失败覆盖）  


## 仍明确不做 / 需上线再做

- Token 仍在 `localStorage`（要 httpOnly Cookie 需后端改登录态）  
- 完整 WAF、设备指纹、支付风控、密钥托管  
- 秒杀库存击穿压测与全局限流策略（网关已有秒杀限流可单独讲）  

面试话术：演示站也把「匿名运营数据、SSRF、点击劫持、富文本 XSS」堵上；生产再上 Cookie 会话与网关级风控。
