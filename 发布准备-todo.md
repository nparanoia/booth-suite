# 发布准备待办（按扫缺报告执行）
> 工作区：`f:\Desktop\booth-suite`（Booth / `com.booth`）。下文若仍写历史工程名，以本目录代码为准。


> 主报告：[`需求扫缺报告.md`](需求扫缺报告.md)（2026-11-13）  
> 原则：**全量补齐该补的**；「不宣称」只用于诚实边界。  
> 状态约定：`[ ]` 未做 / `[~]` 进行中 / `[x]` 完成

---

## 波次 0 — 拍板（先做）

- [x] **X1 写操作**：采用「对话内校验后直写」（与当前代码+简历一致）；废弃确认卡叙事
- [x] **X2 SSE**：`booth-web` 主路径必须接 `/app/chat/stream`；同步接口降级
- [x] **X3 对外措辞**：统一为「校验后直写 + 写熔断 + 指纹/outbox 幂等（波次1补挂）」；禁止再写「确认卡防误触」除非恢复确认流
- [x] 三仓 README 共用「写操作约定」短文定稿 → [`docs/写操作约定.md`](docs/写操作约定.md)
- [x] 「明确不宣称」清单定稿：MCP、多 Agent、RAGAS、完整 OTel、呼叫中心、多店铺、真支付宝沙箱跑通、凭证/试算/退款查询、httpOnly Cookie（演示站）

---

## 波次 1 — 契约与安全闭环（代码优先）

### Agent

- [x] **P0-A1** 直写工具（退货/取消/改地址）统一经 outbox + 参数指纹幂等；写熔断仍生效
- [x] **P0-A2** 静态页 `index.html`：去掉确认卡，走 SSE + 直写结果
- [x] **P0-A3** README「当前范围」与 `ToolNames` 对齐（含改地址、转人工、直写说明）
- [x] **P0-A4** 修订 `personal-merchant-boundary.md` / `security-basics.md` / `p0-hardening.md` 与直写一致
- [x] **P0-A5** `.env` 密钥不入库（example 为空；本工作区无私有 `.env`，无需轮换）；发布前再 secret scan
- [x] 运营指标字段：`confirm*` → `writeReturns` / `writeCancels` / `writeFails`
- [x] phase/extend：`p0-hardening.md` / `PLAN.md` 已标历史/直写口径

### Web

- [x] **P0-W1** `SupportView` 接 SSE（进度 / token / cancel）
- [x] **P0-W2** agent 响应校验业务 `code`（`unwrapAgentResult`）
- [x] **P0-W3** 未启动降级 + 中文错误映射（含 429）；SSE 失败可降级同步
- [x] **P0-W4** 无入库 `dist`；本机 `vue-tsc --noEmit` 已通过（完整 `vite build` 可在有依赖时再跑）
- [x] 开场白/快捷句与「直写」唯一口径一致
- [x] logout 清理 `ql_support_cid`
- [x] 未登录客服入口引导登录（FAB + SupportView）

### 联调门禁（波次 1 完成标准）

- [x] 带单进入 → SSE 进度可见 → 对话内退货成功 → 商城侧有售后单（2026-11-14 联调门禁；后续已切 booth 栈）
- [x] 重复相同退货意图不产生重复脏单（指纹/outbox；二次回复含「未重复建单」，DB 仍 1 条）
- [x] 越权查单被拒（他单返回「订单不存在」且不重试）
- [x] agent `:8091/` 静态演示与 web 客服行为一致（2026-11-14：web 登录→客服 SSE/政策+知识来源；API 门禁同步过）

---

## 本地联调约定（品牌清理）

> 2026-11-14：已停掉本机 `mall-*` 栈，改用套件 `booth-cloud` / `booth-agent` compose（库 `booth_*` / `booth_agent`，容器 `booth-*`）。旧 mall 数据卷未迁入；演示数据由 Flyway 种子 + DEMO-DATA 重建。

- [x] **上线前-品牌清理**：`booth_user` / `booth_product` / `booth_trade`；Agent PG `booth` / `booth_agent`
- [x] **上线前-镜像重建**：`booth-app-*` / `booth-agent-*` 已替换 `mall-*`
- [x] **上线前-文档/脚本**：去掉过渡「连 mall_*」说明；`.env.example` 保持 booth 默认
- [x] **上线前-扫仓**：公开文档/脚本/compose 无 `ssy.mall`；剩余 `mall` 仅历史验收记录字样（见下进度摘要）

---

## 波次 2 — 开箱与仓库门面

### Cloud

- [x] **P0-C1** 根 README + LICENSE（booth 目标态；过渡说明指向套件 todo）
- [x] **P0-C2** compose：`booth-search` + profile `search`/`full`；默认最小栈不含搜索并已写明
- [x] **P0-C3** `docs/GETTING-STARTED.md`：build / IK / 排障；`pull_policy: missing`
- [x] **P0-C4** `deploy/gen-env.ps1` + `gen-env.sh`；`.env.example` 禁止空 token 蒙混
- [x] **P0-C5** `start-apps.ps1` 已在 deploy 并写入 GETTING-STARTED
- [x] **P1-D1** `booth-product` `V2__seed_demo_catalog.sql`
- [x] **P1-D2** `docs/DEMO-DATA.md`
- [x] 端口矩阵 + 启动顺序；链到 web/agent

### Agent / Web 仓库

- [ ] git init（agent、web）；cloud 补远端（待你明确要求再做）
- [x] `.gitignore` 扩展（`.env` / 密钥后缀等）
- [x] LICENSE（三仓 + 套件根）
- [x] web README：两档启动、nginx 示例、`.env.example`
- [x] 三仓顶部互链 + cloud「5 分钟路径」

---

## 波次 3 — 安全硬化与对外材料

- [x] **P1-S1** SECURITY.md + 生产换密清单 + inner 威胁模型说明（套件根 + 三仓）
- [x] **P1-S2** compose 默认关 mock 支付 / ALLOW_DEV_SECRET（`.env` / `start-apps.ps1` 本机打开）
- [x] **P1-S3** Grafana 匿名默认关；Knife4j 经 `API_DOCS_ENABLED`；prometheus/中间件「勿公网」写入 SECURITY
- [x] **P1-S4** Agent swagger：`BOOTH_SWAGGER_ENABLED`（默认 true，生产关）
- [x] **P1-S5** CSP/frame-ancestors + localStorage 风险写入 booth-web SECURITY.md
- [x] **P1-S7** 全仓扫密报告 [`docs/secret-scan-20261114.txt`](docs/secret-scan-20261114.txt)；cloud 若已有远端历史建议再用 gitleaks 扫一遍
- [x] **P1-M1** 支付宣称改为 Mock + 预留（README / DEMO-DATA / SECURITY 已对齐）
- [x] **P1-M2** `docs/metrics/` 脱敏黄金/联调指标样例
- [x] **P1-M4** 简历 SSE「约 50%」改为定性（写操作约定 + metrics README）
- [x] AI 应用能力文档成稿（[`docs/能力说明.md`](docs/能力说明.md)；架构×1 + 映射/安全/评测；可再拆多图画册）
- [x] Demo 文字脚本（`docs/demo/脚本-客服直写冒烟.md`）；录屏/GIF 待补
- [ ] CI badge；Release tag + CHANGELOG

---

## 波次 4 — 验收

- [x] 干净环境按 GETTING-STARTED 从零跑通（本机已用 booth compose 新卷；报告见 [`docs/wave4-acceptance.md`](docs/wave4-acceptance.md)）
- [x] 按「宣称清单」冒烟 + 安全边界加压（API PASS；改地址话术弱；Swagger/actuator/prometheus 演示暴露已记录）
- [x] 公开前 PII/密钥：见 `docs/secret-scan-20261114.txt`（发布前建议再扫）
- [x] 默认口令 README 醒目标「仅本地演示」

---

## P2 强建议（波次 3/4 穿插，尽量做）

- [x] 登录限流防撞 admin123（会员/管理 Redis，15 分钟 10 次；2026-11-14 热替换 booth-user 后实测 11 次起 10006）
- [x] web 展示 RAG `sources`（客服气泡折叠）
- [x] compose 最小栈 / 全栈 profile（search/obs/full，波次 2 已做）
- [x] Dependabot（套件根 + 三仓 `.github/dependabot.yml`）
- [x] 后台菜单绑 `can()` + 文案说明演示超管
- [x] 知识库删除确认

---

## 诚实不做（写入对外材料即可）

- 凭证 / 金额试算 / 退款进度查询  
- MCP、多 Agent、RAGAS、完整 OTel、呼叫中心、多店铺  
- 演示站 httpOnly Cookie  
- 真支付宝沙箱（无密钥）  
- perf 未跑的 1000 人秒杀数字不夸大  
- 无预算可不做在线公网 Demo（录屏+本地 Docker）

---

## 进度摘要

| 波次 | 主题 | 状态 |
|------|------|------|
| 0 | 拍板 X1/X2/X3 | 已完成（2026-11-14） |
| 1 | 直写幂等 + SSE + 文档对齐 | 已完成（含浏览器客服点验） |
| 2 | README/compose/种子/git | 文档与 compose 已落地；git init/远端待你下令 |
| — | 上线前 mall→booth 数据/镜像清理 | 已完成（2026-11-14 本机切 booth-*；旧 mall 卷未迁） |
| 3 | SECURITY/能力文档/Demo/CI | 基本完成（S7 已扫；CI badge / 录屏待做） |
| 4 | 干净环境验收 | 已完成（2026-11-14；详见 docs/wave4-acceptance.md） |
