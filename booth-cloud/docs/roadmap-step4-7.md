# booth-cloud 第四步至第七步开发计划

本文档是交给实现者的开发说明，也是验收依据。前三步（工程骨架、基础业务、认证与权限）已完成，后续代码必须沿用已有约定。

每个小节都写了「必须做到」和「验收」。验收不通过的小节视为未完成。

---

## 0. 通用约定（每一步都必须遵守）

### 0.1 代码组织

- 分层：Controller → Service（具体类，不写 Impl 接口）→ Convert（MapStruct）→ Mapper（`BaseMapper`）→ Entity。
- 命名：入参叫 `XxxRequest` / `XxxQuery`，出参叫 `XxxVO`，跨服务传输叫 `XxxDTO`（放在 `booth-api`）。
- 服务间调用只走 `/inner/**`，Feign 接口和 DTO 写在 `booth-api` 对应包下。服务间签名由 `InnerAuthAutoConfiguration` 自动添加，不要自己加。
- 跨服务调用拿结果统一用 `Result.dataOrThrow()`。
- 时间字段由数据库写入，实体继承 `BaseEntity` / `BaseCreateEntity`。

### 0.2 并发与一致性（最容易写错，验收重点）

- 库存、金额、状态、计数类字段在实体上设 `updateStrategy = FieldStrategy.NEVER`，只能用条件 UPDATE 修改，例如：
  `UPDATE ... SET stock = stock - ? WHERE id = ? AND stock >= ?`。
  影响行数为 0 时，按业务失败处理。
- 状态流转一律用 CAS：`UPDATE ... SET status = 新 WHERE id = ? AND status = 旧`。只有更新成功的一方执行后续动作（释放库存、发消息、写状态日志）。
- 不允许「先 select 判断，再 updateById」来改上述字段。
- 所有面向会员的查询和修改，SQL 条件里必须带 `member_id`。
- 远程调用不要放在本地数据库事务里面，避免长事务；需要「本地事务成功后再通知别人」时，用本地消息表（第 4.4 节）。
- 所有 MQ 消费者、所有 `/inner` 写接口都必须幂等，重复调用结果一致。幂等依据写在方法注释里。
- 多行加锁时按主键升序加锁，避免死锁。

### 0.3 安全

- 当前会员 ID 只从 `@RequestHeader(AuthHeaders.MEMBER_ID)` 取，当前管理员 ID 只从 `@RequestHeader(AuthHeaders.ADMIN_ID)` 取。请求体和路径里不接收 memberId。
- 后台接口必须标 `@RequiresPermission`。
- 新权限码写在用户服务新的 Flyway 脚本里（第四步 `V3__seed_trade_permissions.sql`，第六步 `V4__seed_search_permissions.sql`），并分配给 `PRODUCT_OPERATOR` 以外的合适角色（默认只给超级管理员，超级管理员本来就是 `*`）。
- 第三方回调（支付宝异步通知）放在 `/open/**`，靠验签保护，不靠登录。
- 密钥、证书、AppId 只能来自环境变量或 Nacos，不允许提交到仓库。

### 0.4 数据库与错误码

- 禁止修改任何已有的 `V1__*.sql` / `V2__*.sql`（Flyway 校验和会失败）。表结构变更只能加新版本脚本。
- 第四、五步需要的表都已在 `booth_trade` 的 V1 中建好：`trade_order`、`trade_order_item`、`order_status_log`、`payment_record`、`order_return_apply`、`coupon`、`coupon_scope`、`member_coupon`、`seckill_*`、`local_message`。商品服务已有 `stock_lock_record`，用户服务已有 `member_integration_log`。实体和 Mapper 也已存在。优先用现有表，确需加字段时写新迁移并说明理由。
- 错误码分段：

  | 范围 | 用途 |
  |---|---|
  | 用户 103xx | 积分 |
  | 商品 204xx | 库存锁定 |
  | 交易 302xx | 订单 |
  | 交易 303xx | 支付 |
  | 交易 304xx | 优惠券 |
  | 交易 305xx | 秒杀 |
  | 搜索 4xxxx | 搜索 |

### 0.5 交付方式

- 一个小节一次提交，提交信息写清楚是哪一节，例如 `4.1 库存锁定/扣减/释放`。
- 每个小节交付三样东西：代码；新增的 Flyway 脚本（如有）；在 `logs/e2e.ps1` 末尾追加该小节的检查项。
- 不允许删除或放宽 e2e 已有的检查项。
- 每个服务新增 Mapper 后，`MapperSchemaIT` 必须仍然通过。
- 实现中如果必须偏离本文档，在本文件末尾「偏离记录」中写明原因。

---

## 第四步：交易核心

建议顺序：4.1 → 4.2 → 4.4 → 4.3 → 4.5 → 4.6 → 4.7。4.4 是关单和支付后续动作的基础，要先于 4.3 的取消和关单实现。

### 4.1 库存锁定、扣减、释放（商品服务）

**接口**：写在 `booth-api` 的 `product/StockInnerClient`。三个接口都用 `orderSn` 作幂等键。

| 接口 | 作用 |
|---|---|
| `POST /inner/stock/lock`，`{orderSn, items:[{skuId, quantity}]}` | 下单时锁定库存 |
| `POST /inner/stock/deduct`，`{orderSn}` | 支付成功后真正扣减 |
| `POST /inner/stock/release`，`{orderSn}` | 取消、超时关单后释放 |

**必须做到**：

- **lock**：一个事务内完成；items 按 skuId 升序处理。
  1. 先插入 `stock_lock_record(status=LOCKED)`。
  2. 再执行 `UPDATE sku SET lock_stock = lock_stock + ? WHERE id = ? AND stock - lock_stock >= ?`。
  3. 任一 SKU 影响行数为 0，整单回滚，返回库存不足（错误码带上 skuId）。
  4. 同时校验商品处于上架状态。
- **lock 幂等**：同一 orderSn 重复调用时，记录已全部是 LOCKED 就直接返回成功；记录是 RELEASED 时返回「订单已取消，不能再锁定」。
- **deduct**：逐条把记录 CAS 为 DEDUCTED（`WHERE status = LOCKED`）。只有 CAS 成功的那条，才执行 `stock -= q, lock_stock -= q, sale += q`，并累加 `product.sale`。
- **release**：逐条把记录 CAS 为 RELEASED（`WHERE status = LOCKED`），成功的才执行 `lock_stock -= q`。
- **空回滚和防悬挂**：release 时如果该 orderSn 一条记录都没有，按请求的 skuId 插入 RELEASED 状态的记录。这样晚到的 lock 会因为唯一键冲突而被拒绝。release 请求体因此需要带 items（可选字段）。
- **对账任务**：每 5 分钟扫描 LOCKED 且创建超过「支付超时 + 10 分钟」的记录。调用交易服务 `GET /inner/orders/{orderSn}/status` 查询订单：
  - 订单不存在或已关闭 → 执行 release；
  - 订单已支付 → 执行 deduct；
  - 每次处理写日志。

  交易服务的这个 inner 接口在 4.2 一起提供。

**验收**：

- 库存 10，50 个并发请求各锁 1 件：恰好 10 个成功，`lock_stock = 10`，且 `stock - lock_stock >= 0` 始终成立。
- 同一 orderSn 重复 lock、deduct、release 各 3 次，库存数字只变化一次。
- 先 release 再 lock 同一 orderSn，lock 被拒绝。

### 4.2 下单（交易服务；用户服务补积分接口）

**用户服务的积分接口**（写在 `booth-api` 的 `user/IntegrationInnerClient`）：

- `POST /inner/members/{memberId}/integration/deduct`，`{bizNo, amount}`：在 `member_integration_log` 用 `(biz_type, biz_no)` 唯一键去重，然后执行条件 UPDATE：`integration = integration - ? WHERE integration >= ?`。
- `POST /inner/members/{memberId}/integration/refund`，`{bizNo, amount}`：同样用唯一键去重。只有存在对应扣减记录时才返还。
- `POST /inner/members/{memberId}/integration/reward`，`{bizNo, amount}`：确认收货后赠送积分。

**交易服务的会员接口**：

| 接口 | 作用 |
|---|---|
| `POST /app/orders/confirm` | 确认页：根据购物车已勾选项或「立即购买 {skuId, quantity}」，返回商品快照、可用优惠券、可用积分、运费（固定 0）、应付金额，以及一次性的 `submitToken` |
| `POST /app/orders` | 提交订单：`{source: CART \| DIRECT, items?, addressId, memberCouponId?, useIntegration, note, submitToken}` |

**提交订单的流程**（顺序不要改）：

1. **防重复提交**：用 `GETDEL` 原子取出 Redis `booth:order:token:{memberId}:{token}`，取不到就返回「请勿重复提交」。
2. 通过 `MemberInnerClient` 取收货地址，通过 `SkuInnerClient` 取实时价格，重新计算金额。前端传来的金额一律不信。
3. **计算金额**：用 `BigDecimal`，保留 2 位，`HALF_UP`。
   - 优惠券和积分抵扣按明细金额比例分摊，最后一行承担尾差。
   - 积分规则写成常量：100 积分抵 1 元，抵扣不超过商品总额的 50%。
   - 应付金额不得小于 0.01。
4. **锁优惠券**：条件 UPDATE `member_coupon SET status = LOCKED, order_sn = ? WHERE id = ? AND member_id = ? AND status = UNUSED`，并校验使用时间、门槛、适用范围。
5. 扣积分（bizNo = orderSn）。
6. 锁库存（第 4.1 节）。
7. **本地事务**：插入 `trade_order`（状态待付款）、`trade_order_item`、`order_status_log`，写入延迟关单消息（第 4.4 节）。来源是购物车时，在同一事务里删除对应的购物车行。
8. **补偿**：第 4 至 7 步任何一步失败，按相反顺序补偿已经成功的步骤：释放库存、返还积分、解锁优惠券。补偿调用失败要记日志，交给 4.1 的对账任务和 4.4 的重试兜底。

**订单号**：用 MyBatis-Plus 的 `IdWorker` 生成。`trade_order.id` 已是雪花 ID，`order_sn` 用它的字符串形式加日期前缀。

**同时提供** `GET /inner/orders/{orderSn}/status`，供 4.1 的对账任务使用。

**验收**：

- 正常下单后，订单金额等于各明细实付之和；库存锁定数正确；购物车对应行被删除。
- 同一 submitToken 并发提交 5 次，只生成 1 个订单。
- 库存不足时下单失败，并且优惠券、积分都恢复原状。
- 篡改请求里的价格无效。
- 用别人的地址 ID 下单失败。

### 4.3 订单状态机与会员、后台接口

**状态**：0 待付款 → 1 待发货 → 2 已发货 → 3 已完成；0 → 4 已关闭。

| 操作 | 流转 | 谁触发 | 成功后的动作 |
|---|---|---|---|
| 支付成功 | 0→1 | 支付回调 | 发「支付成功」消息：扣减库存、核销优惠券 |
| 会员取消 | 0→4 MEMBER_CANCEL | 会员 | 发「订单关闭」消息：释放库存、解锁优惠券、返还积分 |
| 超时关闭 | 0→4 TIMEOUT | 延迟消息 / 兜底扫描 | 同上 |
| 后台关闭 | 0→4 ADMIN_CLOSE | 管理员 | 同上 |
| 发货 | 1→2 | 管理员，填物流公司和单号 | 无 |
| 确认收货 | 2→3 | 会员；或发货后 14 天自动确认（定时任务） | 发「订单完成」消息：赠送积分 |

**必须做到**：

- 每次流转都是一条 CAS UPDATE。影响行数为 0 时返回「订单状态已变化」。
- 成功后，在同一事务里写 `order_status_log` 和本地消息。
- 支付回调与超时关单同时发生时，只能有一个成功。如果关单先成功、钱后到，按 4.5 的「关闭后到账」处理。

**会员接口**（都带 member_id 条件）：

- `GET /app/orders?status=`：分页
- `GET /app/orders/{orderSn}`
- `POST /app/orders/{orderSn}/cancel`
- `POST /app/orders/{orderSn}/receive`
- `DELETE /app/orders/{orderSn}`：只隐藏已完成或已关闭的订单，设置 `member_deleted = 1`

**后台接口**：

- `GET /admin/orders`、`GET /admin/orders/{orderSn}`：权限 `trade:order:view`
- `POST /admin/orders/{orderSn}/ship`、`POST /admin/orders/{orderSn}/close`：权限 `trade:order:edit`

**验收**：

- 同一个待付款订单，并发执行「取消」和「模拟支付」各 10 次：最终只有一种结果生效，状态日志恰好一条，库存只释放或只扣减一次。
- 会员 A 操作会员 B 的订单失败。

### 4.4 本地消息表与 RabbitMQ

**组件**：交易服务新增 `LocalMessageService`。

- `save(bizType, bizKey, exchange, routingKey, payload, delay)`：必须在业务事务内调用。
- 事务提交后（`TransactionSynchronization.afterCommit`）立即投递一次。

**必须做到**：

- **Publisher Confirm**：开启 `publisher-confirm-type: correlated`，`publisher-returns: true`。correlationId 用 messageId；收到 ack 后把消息置为已确认。
- **重投任务**：每 10 秒扫描「待确认且 `next_retry_time < now`」的消息重投，指数退避。超过 8 次置为「超过重试上限」并打 ERROR 日志。扫描时用条件 UPDATE 抢占，或在 `next_retry_time` 上加租约，避免多实例重复投递。
- **拓扑**（在配置类中声明）：

  | 交换机 | 类型 | 绑定的队列 |
  |---|---|---|
  | `booth.order.delay` | `x-delayed-message`（插件已装） | `booth.order.timeout` |
  | `booth.order.event` | topic | `booth.order.paid`、`booth.order.closed`、`booth.order.completed` |

  每个队列配置死信队列。
- **消费者**：手动 ack。业务异常重试 3 次后进死信。所有消费逻辑必须幂等（依据：订单状态 CAS、`stock_lock_record` 状态、积分流水唯一键、`member_coupon` 状态 CAS）。
- **支付超时**：配置项 `booth.order.pay-timeout`，默认 `30m`。e2e 环境通过环境变量设成 `60s`。
- **兜底扫描**：每分钟扫描超时未付款的订单（使用 `idx_status_time` 索引）执行关单，防止延迟消息丢失。

**验收**：

- 把超时设为 60 秒，下单不付款：约 60 秒后订单关闭、库存释放、优惠券和积分恢复。
- 停掉 RabbitMQ 时下单仍然成功，消息停在待确认；恢复后自动投递并被确认。
- 同一条消息手动重复投递 3 次，结果不变。

### 4.5 支付（支付宝沙箱）

**抽象**：定义 `PaymentGateway` 接口，有两个实现。

- `AlipayGateway`：使用 alipay-sdk-java 的 `alipay.trade.page.pay`、`alipay.trade.query`。配置来自环境变量。
- `MockPaymentGateway`：只在 `booth.pay.mock-enabled=true` 时注册，默认关闭，供 e2e 使用。它的异步通知用 HMAC 签名，模拟真实验签流程。

**接口**：

| 接口 | 作用 |
|---|---|
| `POST /app/orders/{orderSn}/pay` | 校验订单属于本人且待付款；按 `(order_sn, pay_type)` 唯一键插入或复用 `payment_record`；返回支付页地址或表单 |
| `POST /open/pay/alipay/notify` | 支付宝异步通知 |
| `POST /open/pay/mock/notify` | 只在 mock 开启时存在 |

**异步通知必须做到**（顺序不要改）：

1. 验签（`AlipaySignature.rsaCheckV1`），校验 app_id。
2. 校验 `out_trade_no` 对应的订单存在。
3. 校验 `total_amount` 与订单 `pay_amount` 相等，用 `compareTo`。
4. 只处理 `TRADE_SUCCESS` / `TRADE_FINISHED`。
5. 同一事务内：
   - 更新 `payment_record`，用 `uk_channel_trade_no` 去重；
   - 订单 CAS 0→1，写入支付时间和支付方式；
   - 写「支付成功」本地消息。
6. 返回纯文本 `success`。验签失败或金额不符时返回 `failure`，并打 WARN 日志。
7. **关闭后到账**：订单已关闭但收到支付成功通知时，不改订单状态。把 `payment_record` 标记为需退款（需要新增状态值，写新迁移），并打 ERROR 日志。调用退款接口是可选项。

**主动查询任务**：对「已发起支付、未收到通知、创建超过 5 分钟」的记录调用 `trade.query` 补单，走同一段处理逻辑。

**验收**（用 mock）：

- 正确通知后订单变为待发货，库存从锁定转为扣减，优惠券变为已使用。
- 重复通知 5 次只处理一次。
- 金额被篡改、签名错误的通知都被拒绝，订单不变。
- 已关闭订单收到通知时，订单保持关闭，并产生需退款记录。

### 4.6 优惠券

**后台接口**（权限 `trade:coupon:view` / `trade:coupon:edit`）：

- 优惠券模板的增删改查、发布、下线；适用范围维护。
- 已发布的券不能修改面额和门槛。

**会员接口**：

- `GET /open/coupons`：可领取列表
- `POST /app/coupons/{id}/receive`：领券
- `GET /app/coupons?status=`：我的券

**领券必须做到**：

- 在一个事务里，先执行 `UPDATE coupon SET remain_count = remain_count - 1 WHERE id = ? AND status = 已发布 AND remain_count > 0 AND now() BETWEEN receive_start_time AND receive_end_time`。
- 成功后再统计该会员已领张数；超过 `per_limit` 就抛异常回滚。
- 因为前面的 UPDATE 持有该券的行锁，同一张券的并发领取会串行执行，每人限领不会被并发突破。这一点要写在方法注释里。
- **过期任务**：每天把过期未使用的会员券置为已过期。

**验收**：

- 发行 10 张、每人限 1 张：20 个会员并发领取，恰好 10 人成功。
- 同一会员并发领 10 次，只成功 1 次。
- 下单使用后，券变为已锁定；取消订单后恢复为未使用；支付后变为已使用。

### 4.7 售后（优先级最低，可放到第五步之后）

- 会员对已完成订单的明细申请退货。依靠 `uk_order_item` 唯一键防止重复申请。
- 后台审核、确认收货。审批状态流转用 CAS。退款只做记录，不调用渠道。

**验收**：重复申请被拒绝；状态只能按顺序推进。

### 第四步整体验收

- 全量构建通过，三个服务的 `MapperSchemaIT` 通过，e2e 全绿。
- **压测**：JMeter 计划 `perf/order.jmx`，200 个会员、库存 100，并发下单。下单成功数恰好 100；`stock` 与 `lock_stock` 之差和订单明细数量一致。QPS 和 RT 记录到 `docs/perf.md`。

---

## 第五步：秒杀

### 5.1 后台配置与预热

**后台接口**（权限 `trade:seckill:view` / `trade:seckill:edit`）：

- 活动、场次、秒杀商品的增删改查。
- 秒杀商品引用真实 SKU。保存时校验 SKU 存在，并且 `seckill_stock` 不超过当前可售库存。

**预热任务**：场次开始前 10 分钟，把秒杀商品写入 Redis。key 统一使用 `booth:seckill:` 前缀：

| key | 内容 |
|---|---|
| `stock:{seckillSkuId}` | 剩余秒杀库存 |
| `info:{seckillSkuId}` | Hash：价格、每人限购、开始时间、结束时间、skuId |
| `bought:{seckillSkuId}` | Hash：memberId → 已购件数 |

所有 key 的过期时间都设到场次结束后 1 天。预热要幂等，已预热的不能覆盖剩余库存。

### 5.2 抢购接口

**会员接口**：

| 接口 | 作用 |
|---|---|
| `GET /open/seckill/sessions/current` | 当前场次 |
| `GET /open/seckill/skus/{id}` | 秒杀商品详情 |
| `POST /app/seckill/{id}/path` | 活动开始后才返回一次性随机路径，存为 `booth:seckill:path:{memberId}:{id}`，有效期 60 秒，用于隐藏抢购地址 |
| `POST /app/seckill/{id}/orders` | 抢购，`{path, quantity, addressId}` |
| `GET /app/seckill/results/{requestId}` | 轮询结果：排队中、成功（带 orderSn）、失败（带原因） |

**抢购必须做到**：

1. 校验并删除 path（原子操作）。
2. **Lua 脚本**原子完成以下步骤，返回不同的结果码：
   1. 校验当前时间在活动窗口内；
   2. 校验「已购 + 本次 ≤ 每人限购」；
   3. 校验库存 ≥ 本次数量；
   4. `DECRBY` 库存；
   5. `HINCRBY` 已购件数。

   脚本放在 `resources/lua/` 下，用 `DefaultRedisScript` 加载。
3. 成功后生成 requestId，把结果写成排队中，然后发送 MQ 消息到 `booth.seckill.order`。发送使用 confirm 机制；确认发送失败时，执行反向 Lua，把库存和已购件数加回去，并返回失败。
4. 直接返回 requestId，不等待下单完成。

**消费者**：

- 依靠 `seckill_order` 的 `uk_sku_member` 唯一键保证幂等。
- 复用 4.2 的下单核心逻辑：`order_type = 1`，单价为秒杀价，不能用优惠券和积分。
- 在 DB 中条件扣减 `seckill_sku.seckill_stock`，插入 `seckill_order`，写延迟关单消息。
- 成功后把结果写为成功。
- 失败分两种：业务失败（例如真实库存不足）时执行反向 Lua，结果写为失败；系统异常时重试，最终进死信，由人工或对账处理。

**秒杀订单关闭**：在 4.3 的关单动作之外，还要归还 DB 中的 `seckill_stock`、Redis 中的库存和已购件数，可以复用反向 Lua。

### 5.3 限流与对账

**Sentinel**：

- 网关引入 Sentinel Gateway 适配，对秒杀路由按 QPS 限流。
- 交易服务的抢购方法用 `@SentinelResource` 做热点参数限流，参数是 seckillSkuId。
- 被限流时返回 HTTP 429，Body 为统一 `Result` JSON。
- 规则可以先写在配置里，可选接入 Nacos 数据源。
- 在 compose 中添加 sentinel-dashboard 容器。

**对账**：场次结束后，以及后台手动触发的 `POST /admin/seckill/skus/{id}/reconcile`，比较以下三个数，不一致时打 ERROR 日志并在接口中返回差异：

- Redis 已售数 = 初始库存 − 剩余库存；
- DB 中 `seckill_order` 的件数之和；
- 未关闭的秒杀订单件数。

### 第五步验收

- 秒杀库存 100，1000 个会员（脚本预先注册并登录）用 JMeter `perf/seckill.jmx` 抢购：
  - 成功订单恰好 100，没有超卖；
  - 同一会员没有超过限购；
  - Redis 库存不小于 0，DB `seckill_stock` 不小于 0；
  - 对账结果无差异。
- 活动开始前请求 path 被拒绝；path 重复使用被拒绝。
- 秒杀订单超时关闭后，Redis 库存加回，该会员可以再抢。
- 超过限流阈值时出现 429，服务没有崩溃。
- QPS、RT、成功率写入 `docs/perf.md`，简历中的 x 占位用这里的数据填写。

---

## 第六步：搜索与缓存

### 6.1 Elasticsearch 索引与搜索接口（`booth-search`）

**索引**：

- 真实索引名为 `product_v{时间戳}`，对外只使用别名 `product`。
- 映射：

  | 字段 | 类型 |
  |---|---|
  | `name` | text，`ik_max_word` 建索引，`ik_smart` 搜索 |
  | `subTitle`、`keywords` | text |
  | `brandId`、`categoryId` | long |
  | `brandName`、`categoryName` | keyword |
  | `price` | scaled_float，scaling_factor 100 |
  | `sale` | integer |
  | `attrs` | nested，包含 name、value |
  | `pic` | 不建索引 |
  | `createTime` | date |
  | `updateTime` | date，用作外部版本号 |

- 映射文件放在 `resources/es/product-mapping.json`。

**接口**：`GET /open/search/products`

- 参数：`keyword`、`categoryId`、`brandId`、`priceMin`、`priceMax`、`attrs`、`sort`（综合 / 销量 / 价格升 / 价格降 / 新品）、`pageNum`、`pageSize`。
- 返回：高亮的商品名、品牌聚合、分类聚合、属性聚合。
- 限制：`pageNum × pageSize ≤ 10000`，超出时返回参数错误。

**商品服务提供** `POST /inner/products/search-docs`：根据 ID 列表返回组装好的索引文档（上架且未删除的商品）。另提供按 ID 游标分页的全量导出接口，供重建索引使用。

### 6.2 Canal 增量同步

**Canal 服务**：在 compose 中添加 canal-server 容器，使用 RabbitMQ 模式。

- 订阅的表：`booth_product` 库的 `product`、`sku`、`product_attr_value`、`brand`、`category`。
- MySQL 已开启 ROW 格式 binlog 和 native password；canal 账号在 `deploy/mysql/init` 中已经准备好，以实际文件为准。

**消费者**：

1. 从变更行中解析出受影响的 productId。品牌和分类改名时，按冗余字段影响的商品处理。
2. 批量调用 `search-docs` 获取最新文档。
3. 写入 ES 时使用 `version_type=external_gte`，版本号为 `updateTime` 的毫秒值，防止乱序消息用旧数据覆盖新数据。
4. 商品已下架或已删除时删除对应文档。

依赖说明：`update_time` 由数据库自动维护，条件 UPDATE 也会刷新它，这是第一步就定下的约定，不能破坏。

### 6.3 全量重建与别名切换

**接口**：`POST /admin/search/rebuild`，权限 `search:index:edit`。

**流程**：

1. 创建新索引。
2. 在 Redis 记录「重建中」以及新索引名。
3. 按游标批量导入。
4. 用一次 `_aliases` 请求原子完成：从旧索引移除别名，给新索引加别名。
5. 清除「重建中」标记，删除旧索引。

**重建期间的增量写入**：消费者看到「重建中」标记时，同时写旧索引和新索引，避免重建期间的修改丢失。

**防重入**：同一时间只能有一个重建任务，用 Redis 锁控制。

### 6.4 商品详情多级缓存（商品服务）

**结构**：

- 一级缓存：Caffeine，过期时间 30 秒，限制最大条数。
- 二级缓存：Redis，过期时间 10 分钟 + 随机抖动 0 至 120 秒，避免缓存雪崩。

**必须做到**：

- 不存在的商品 ID 缓存空值 60 秒，防止缓存穿透。
- 热点 key 失效时，只允许一个请求回源，用 Redis 互斥锁或单飞（single-flight）实现，防止缓存击穿。
- 商品、SKU、品牌、分类修改后，在事务提交后删除 Redis 缓存，并通过 Redis Pub/Sub 广播，让所有实例清掉 Caffeine。也可以由 Canal 事件驱动删除，二选一，写在偏离记录里。
- 分类树同样使用两级缓存。
- 前台详情中的可售库存不走缓存，或缓存时间不超过 5 秒。

### 第六步验收

- 新增、修改、下架商品后，5 秒内搜索结果随之变化。
- 乱序重放旧的变更消息不会覆盖新数据。
- 重建索引期间持续修改商品：切换别名后数据完整，搜索接口全程可用。
- 查询不存在的商品 ID 1000 次，数据库只被查询 1 次。
- 修改商品后，详情接口立刻返回新数据（多实例时同样成立）。
- 压测商品详情接口，结果记录到 `docs/perf.md`。

---

## 第七步：可观测性（可选）

- **链路追踪**：Micrometer Tracing 加 Zipkin，或使用 SkyWalking。traceId 写入日志格式，并沿 Feign 和 MQ 消息头传播。
- **指标**：开启 actuator 的 `prometheus` 端点，compose 中加入 Prometheus 和 Grafana。
- **自定义指标**：下单数、下单失败数、秒杀请求数和限流数、本地消息重投数、对账差异数。
- **告警**：本地消息进入「超过重试上限」、对账出现差异、关闭后到账时打 ERROR 日志，并累加计数指标。

**验收**：一次下单请求可以在追踪界面看到网关 → 交易 → 商品、用户的完整链路；Grafana 能看到上述指标。

---

## 验收流程（每个小节完成后）

1. 全量构建，并对每个服务运行 `MapperSchemaIT`。
2. 启动中间件和所有服务，运行 `logs/e2e.ps1`，要求失败数为 0。
3. 代码审查，重点检查：
   - 0.2 节的条件 UPDATE、CAS、member_id 条件；
   - 幂等依据；
   - 远程调用没有放在本地事务中；
   - 补偿顺序；
   - 金额计算全部使用 `BigDecimal`；
   - 后台接口都标了权限注解；
   - 没有提交密钥。
4. 对本节「验收」中的并发场景单独执行脚本或 JMeter，并核对数据库中的数据。

---

## 偏离记录

- 模拟支付默认关闭。本地验收要同时设置 `BOOTH_PAY_MOCK_ENABLED=true` 和 `BOOTH_PAY_ALLOW_DEV_SECRET=true`。开着模拟支付、密钥仍是仓库默认值、又没显式允许时，交易服务拒绝启动。验签顺序仍是先验签、再对金额、再改状态。仓库里没有支付宝沙箱密钥，沙箱支付没有实际跑过。
- 已发布或已下线的优惠券不能删除，只能下线。会员券和订单上还留着券的历史。只有草稿可以连同适用范围一起删。
- 商品服务库存对账的支付超时默认 24 小时，交易服务关单仍是 30 分钟。两边都没配环境变量时，对账只会晚释放，不会在顾客还能付款时把库存放走。
- 优惠分摊在最后一行吃掉尾差之后，如果这一行金额变成负数，就把多出来的券和积分挪到还有剩余货款的行上。券的合计、积分的合计都不变，每行实付不小于 0。
- 延迟关单消息不再强制要求路由成功。延迟插件会对每条超时消息回一条 return，以前会把已经确认的本地消息误报成无法路由。即时消息仍然强制路由；真的退回时标成无法路由，确认回调不能把这个状态改回已确认。
- 积分退回如果发生在扣减提交之前，先写一条金额为 0 的 `ORDER_ABORT`。晚到的扣减看到它就不再改余额。用户服务另外扫描超过 2 分钟、没有退回也没有这个标记的扣减：订单不存在或已关闭才补退，待付款和已支付不动。
- 交易服务按订单状态补优惠券：锁定中的券，订单没有了或已关闭就解锁，已支付就核销，待付款不动。完成赠送积分按最近三天、收货时间从新到旧扫描，调用本身幂等。
- 售后没有新增权限码，审核接口复用 `trade:order:view` / `trade:order:edit`。退款只记状态，不调用支付渠道。
- 库存锁定和积分扣减一旦发出请求，即使调用超时也按订单号补偿。超时只说明调用方没等到结果，商品服务或用户服务仍可能已经提交；释放和退积分都按唯一键幂等。
- 交易和商品服务的 Feign 读超时改为 15 秒，数据库连接池改为 40。并发锁同一行 SKU 时，5 秒内经常等不到锁，调用方先超时，锁定却在之后提交，会出现没有订单的锁定记录。
- 本机没有安装 JMeter。200 人、库存 100 的下单压测用 `logs/step4-accept.ps1` 跑完，成功数恰好 100。`perf/order.jmx` 是同一场景的 JMeter 计划，本机未执行。数字记在 `docs/perf.md`。
- 并发、停 RabbitMQ、压测没有塞进每次都跑的 `logs/e2e.ps1`，因为那段会注册约 220 个会员并停止 RabbitMQ。功能检查仍在 `e2e.ps1`，并发检查在 `logs/step4-accept.ps1`。
- 秒杀权限写在用户服务 `V5`。`V4` 已经用来补充积分流水注释，没有改已执行过的脚本。
- `seckill_order` 原唯一键把已关闭的记录也算进去，超时后会员不能再抢。新增 `active_member`，关闭时置空，唯一键只约束仍有效的一笔。限购大于 1 时也要在同一笔订单里一次买完。
- 秒杀对账只统计未关闭的记录。关闭后 Redis 和数据库库存都已归还，把历史行加进去会永远对不上。
- 抢购消息只有被队列退回时才归还 Redis。确认超时和重试耗尽都说明结果未知，不归还。消费者落单期间会占一个标记，修复任务看到标记就先跳过，避免订单还没写进去就把库存加回。归还用一次性令牌。
- 秒杀限流：网关只限制提交抢购，每秒 25 次。看场次、领地址和查结果不占这个额度，否则轮询结果会把真正的下单请求挤掉。交易服务另按秒杀商品每秒 20 次限制提交。仪表盘容器在 compose 里，默认不连接。
- 秒杀商品增加 `seckill_stock_initial`。对账用初始库存减数据库剩余，和有效秒杀单、未关闭订单比较。Redis 还在时再加 Redis 已售。关单消息还没归还时只记一条信息日志，不算对账错误。Redis 过期后仍按数据库核对。
- 本机没有 JMeter，1000 人抢 100 件没有跑。`perf/seckill.jmx` 留着换机器复现。超卖用 `logs/step5-accept.ps1` 核对：库存 8、20 人同时抢，成功 8，对账一致。数字记在 `docs/perf.md`。功能检查仍在 `logs/e2e.ps1`。
- 搜索权限写在用户服务 `V6`。文档里的 `V4` 已经用于积分流水注释，不能再改。
- 副标题和关键字也用 IK 分词。文档只要求商品名使用 IK，中文副标题和关键字否则对不上。
- 商品详情缓存在商品服务提交成功后删除，并用 Redis 广播清掉各实例内存。搜索索引仍由 Canal 增量维护。可售库存和销量每次查数据库，不放进 10 分钟缓存。
- 本机只跑一个商品服务。第二个实例收到广播后清本地缓存，用单元测试调用同样的处理逻辑验证，不另起进程。乱序版本、重建过程中改商品、1000 次不存在的详情，放在 `logs/step6-accept.ps1`，不放进每次都跑的 `e2e.ps1`。
- Canal 使用 1.1.8。1.1.7 仍执行 `SHOW MASTER STATUS`，MySQL 8.4 这条语句已经不存在，实例起不来。镜像不会把环境变量写进配置，连接信息挂载在 `deploy/canal`。这个版本把 `rabbitmq.deliveryMode` 传给交换机类型，所以写成 `topic`，队列名 `booth.search.canal`，路由键 `booth.product`。
- 链路追踪用 Micrometer Tracing 加 Zipkin，本地采样率默认是 1。秒杀提交次数记在网关；网关限流和交易服务热点限流共用 `booth.seckill.limited`，用 `source` 区分。一次下单的完整链路放在 `logs/step7-accept.ps1`，`e2e.ps1` 只检查下单成功和失败的计数已经出现。
- Feign 自己不会把当前链路带给下游。补上 `feign-micrometer` 之后，同一次下单才同时出现网关、交易、用户和商品。
