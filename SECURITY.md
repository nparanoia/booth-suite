# Security（Booth 套件）

三仓各自维护一份策略，避免拆仓后断链：

| 仓库 | 文档 |
|------|------|
| booth-cloud | [`booth-cloud/SECURITY.md`](booth-cloud/SECURITY.md)（换密清单、默认暴露面、inner 威胁模型） |
| booth-agent | [`booth-agent/SECURITY.md`](booth-agent/SECURITY.md) |
| booth-web | [`booth-web/SECURITY.md`](booth-web/SECURITY.md) |

**作品基线：** 代码/compose 默认关 API 文档与 prometheus；本机用 `gen-env` 打开演示开关。面试口述即可：「默认最小暴露，本地 env 打开联调面。」

写操作安全口径：[`docs/写操作约定.md`](docs/写操作约定.md)。
