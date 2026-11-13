# 评测 / 联调指标样例

| 文件 | 说明 |
|------|------|
| [`golden-metrics.sample.json`](golden-metrics.sample.json) | Agent 离线黄金集指标样例（工具选择 / 拒答 / 直写 / 废弃 confirm） |

## 如何复现

```powershell
cd booth-agent
# 需本机 PG（脚本/conda `.pg` 或 Docker 5433，见 agent README）
mvn -Dtest=GoldenMetricsReportTest test
# 产出：target/golden-metrics.json（gitignored）
```

简历可引用：**任务完成率 / 工具选择准确率** 以该测试报告为准；勿把过期截图当永久数字。

## SSE「感知等待」口径（P1-M4）

**不要**再写未附出处的「约 50%」。对外统一为：

> 客服主路径使用 SSE：先推进度事件（查单 / 检索 / 提交退货等），再推正文 token，降低干等感。

若需量化，另做受控 A/B 并在本目录追加带日期的测量说明。
