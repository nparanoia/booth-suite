-- 交易服务 booth_trade：购物车、订单、支付、售后、优惠券、秒杀、本地消息表

-- (member_id, sku_id) 唯一，加购使用 INSERT ... ON DUPLICATE KEY UPDATE quantity = quantity + ? 原子合并
CREATE TABLE cart_item
(
    id          BIGINT      NOT NULL AUTO_INCREMENT,
    member_id   BIGINT      NOT NULL,
    product_id  BIGINT      NOT NULL,
    sku_id      BIGINT      NOT NULL,
    quantity    INT         NOT NULL,
    checked     TINYINT     NOT NULL DEFAULT 1,
    create_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    update_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_member_sku (member_id, sku_id),
    CONSTRAINT ck_cart_quantity CHECK (quantity > 0)
) ENGINE = InnoDB COMMENT ='购物车';

-- 状态流转统一使用 UPDATE ... WHERE id = ? AND status = ? 的 CAS 方式，只有更新成功的一方继续执行后续动作
CREATE TABLE trade_order
(
    id                      BIGINT         NOT NULL COMMENT '雪花 ID，不暴露订单量且便于后续分库分表',
    order_sn                VARCHAR(32)    NOT NULL,
    member_id               BIGINT         NOT NULL,
    member_username         VARCHAR(64)    NOT NULL,
    order_type              TINYINT        NOT NULL DEFAULT 0 COMMENT '0普通 1秒杀',
    status                  TINYINT        NOT NULL COMMENT '0待付款 1待发货 2已发货 3已完成 4已关闭',
    total_amount            DECIMAL(12, 2) NOT NULL COMMENT '商品总额',
    freight_amount          DECIMAL(12, 2) NOT NULL DEFAULT 0,
    coupon_amount           DECIMAL(12, 2) NOT NULL DEFAULT 0,
    integration_amount      DECIMAL(12, 2) NOT NULL DEFAULT 0,
    pay_amount              DECIMAL(12, 2) NOT NULL COMMENT '应付金额',
    member_coupon_id        BIGINT                  DEFAULT NULL,
    use_integration         INT            NOT NULL DEFAULT 0 COMMENT '下单抵扣使用的积分',
    reward_integration      INT            NOT NULL DEFAULT 0 COMMENT '确认收货后赠送的积分',
    pay_type                TINYINT                 DEFAULT NULL COMMENT '1支付宝 2微信',
    payment_time            DATETIME(3)             DEFAULT NULL,
    receiver_name           VARCHAR(64)    NOT NULL,
    receiver_phone          VARCHAR(20)    NOT NULL,
    receiver_province       VARCHAR(32)    NOT NULL,
    receiver_city           VARCHAR(32)    NOT NULL,
    receiver_region         VARCHAR(32)    NOT NULL,
    receiver_detail_address VARCHAR(200)   NOT NULL,
    receiver_post_code      VARCHAR(16)             DEFAULT NULL,
    note                    VARCHAR(500)            DEFAULT NULL,
    delivery_company        VARCHAR(64)             DEFAULT NULL,
    delivery_sn             VARCHAR(64)             DEFAULT NULL,
    delivery_time           DATETIME(3)             DEFAULT NULL,
    receive_time            DATETIME(3)             DEFAULT NULL,
    close_time              DATETIME(3)             DEFAULT NULL,
    close_reason            VARCHAR(32)             DEFAULT NULL COMMENT 'TIMEOUT / MEMBER_CANCEL / ADMIN_CLOSE',
    member_deleted          TINYINT        NOT NULL DEFAULT 0 COMMENT '会员侧隐藏，后台仍可见',
    create_time             DATETIME(3)    NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    update_time             DATETIME(3)    NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_order_sn (order_sn),
    KEY idx_member_time (member_id, create_time),
    KEY idx_member_status_time (member_id, status, create_time),
    -- 超时关单兜底扫描：status = 0 AND create_time < ?
    KEY idx_status_time (status, create_time)
) ENGINE = InnoDB COMMENT ='订单';

CREATE TABLE trade_order_item
(
    id                 BIGINT         NOT NULL AUTO_INCREMENT,
    order_id           BIGINT         NOT NULL,
    order_sn           VARCHAR(32)    NOT NULL,
    product_id         BIGINT         NOT NULL,
    sku_id             BIGINT         NOT NULL,
    product_name       VARCHAR(200)   NOT NULL,
    product_pic        VARCHAR(500)            DEFAULT NULL,
    sku_code           VARCHAR(64)    NOT NULL,
    spec_data          JSON           NOT NULL,
    brand_name         VARCHAR(64)             DEFAULT NULL,
    category_id        BIGINT         NOT NULL,
    price              DECIMAL(10, 2) NOT NULL COMMENT '下单时单价快照',
    quantity           INT            NOT NULL,
    coupon_amount      DECIMAL(10, 2) NOT NULL DEFAULT 0 COMMENT '优惠券分摊',
    integration_amount DECIMAL(10, 2) NOT NULL DEFAULT 0 COMMENT '积分分摊',
    real_amount        DECIMAL(10, 2) NOT NULL COMMENT '分摊优惠后的实付金额',
    create_time        DATETIME(3)    NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    KEY idx_order_id (order_id)
) ENGINE = InnoDB COMMENT ='订单明细';

CREATE TABLE order_status_log
(
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    order_id      BIGINT       NOT NULL,
    from_status   TINYINT               DEFAULT NULL,
    to_status     TINYINT      NOT NULL,
    operator_type TINYINT      NOT NULL COMMENT '1会员 2系统 3管理员',
    operator      VARCHAR(64)  NOT NULL,
    note          VARCHAR(200)          DEFAULT NULL,
    create_time   DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    KEY idx_order_id (order_id)
) ENGINE = InnoDB COMMENT ='订单状态变更记录';

-- 支付宝异步通知以 (pay_type, channel_trade_no) 去重，验签后还需校验金额与订单号
CREATE TABLE payment_record
(
    id               BIGINT         NOT NULL AUTO_INCREMENT,
    order_sn         VARCHAR(32)    NOT NULL,
    pay_type         TINYINT        NOT NULL COMMENT '1支付宝 2微信',
    amount           DECIMAL(12, 2) NOT NULL,
    channel_trade_no VARCHAR(64)             DEFAULT NULL COMMENT '渠道交易号，支付成功后回填',
    status           TINYINT        NOT NULL DEFAULT 0 COMMENT '0待支付 1支付成功 2已关闭',
    notify_time      DATETIME(3)             DEFAULT NULL,
    create_time      DATETIME(3)    NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    update_time      DATETIME(3)    NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_order_pay_type (order_sn, pay_type),
    UNIQUE KEY uk_channel_trade_no (pay_type, channel_trade_no)
) ENGINE = InnoDB COMMENT ='支付记录';

CREATE TABLE order_return_apply
(
    id              BIGINT         NOT NULL AUTO_INCREMENT,
    order_id        BIGINT         NOT NULL,
    order_item_id   BIGINT         NOT NULL,
    order_sn        VARCHAR(32)    NOT NULL,
    member_id       BIGINT         NOT NULL,
    return_quantity INT            NOT NULL,
    return_amount   DECIMAL(10, 2) NOT NULL,
    reason          VARCHAR(64)    NOT NULL,
    description     VARCHAR(500)            DEFAULT NULL,
    proof_pics      VARCHAR(2000)           DEFAULT NULL COMMENT '凭证图片，逗号分隔',
    status          TINYINT        NOT NULL DEFAULT 0 COMMENT '0待处理 1退货中 2已完成 3已拒绝',
    handle_admin    VARCHAR(64)             DEFAULT NULL,
    handle_note     VARCHAR(500)            DEFAULT NULL,
    handle_time     DATETIME(3)             DEFAULT NULL,
    receive_admin   VARCHAR(64)             DEFAULT NULL,
    receive_note    VARCHAR(500)            DEFAULT NULL,
    receive_time    DATETIME(3)             DEFAULT NULL,
    create_time     DATETIME(3)    NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    update_time     DATETIME(3)    NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    -- 一个订单明细只允许一条售后申请，防止重复提交
    UNIQUE KEY uk_order_item (order_item_id),
    KEY idx_member_time (member_id, create_time),
    KEY idx_status_time (status, create_time)
) ENGINE = InnoDB COMMENT ='售后申请';

-- 剩余数量 remain_count 通过 UPDATE ... SET remain_count = remain_count - 1 WHERE remain_count > 0 原子扣减
CREATE TABLE coupon
(
    id                 BIGINT         NOT NULL AUTO_INCREMENT,
    name               VARCHAR(100)   NOT NULL,
    amount             DECIMAL(10, 2) NOT NULL COMMENT '面额',
    min_point          DECIMAL(10, 2) NOT NULL DEFAULT 0 COMMENT '使用门槛，0 为无门槛',
    use_type           TINYINT        NOT NULL DEFAULT 0 COMMENT '0全场通用 1指定分类 2指定商品',
    total_count        INT            NOT NULL COMMENT '发行总量',
    remain_count       INT            NOT NULL COMMENT '剩余可领数量',
    per_limit          INT            NOT NULL DEFAULT 1 COMMENT '每人限领',
    receive_start_time DATETIME(3)    NOT NULL,
    receive_end_time   DATETIME(3)    NOT NULL,
    use_start_time     DATETIME(3)    NOT NULL,
    use_end_time       DATETIME(3)    NOT NULL,
    status             TINYINT        NOT NULL DEFAULT 0 COMMENT '0未发布 1已发布 2已下线',
    note               VARCHAR(200)            DEFAULT NULL,
    create_time        DATETIME(3)    NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    update_time        DATETIME(3)    NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    KEY idx_status_receive (status, receive_start_time),
    CONSTRAINT ck_coupon_count CHECK (remain_count >= 0 AND remain_count <= total_count)
) ENGINE = InnoDB COMMENT ='优惠券模板';

CREATE TABLE coupon_scope
(
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    coupon_id   BIGINT       NOT NULL,
    scope_type  TINYINT      NOT NULL COMMENT '1分类 2商品',
    target_id   BIGINT       NOT NULL,
    target_name VARCHAR(200) NOT NULL,
    create_time DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_coupon_target (coupon_id, scope_type, target_id)
) ENGINE = InnoDB COMMENT ='优惠券适用范围';

-- 会员券状态与库存同构：下单锁定、支付核销、取消释放
CREATE TABLE member_coupon
(
    id          BIGINT      NOT NULL AUTO_INCREMENT,
    coupon_id   BIGINT      NOT NULL,
    member_id   BIGINT      NOT NULL,
    get_type    TINYINT     NOT NULL COMMENT '0后台发放 1主动领取',
    status      TINYINT     NOT NULL DEFAULT 0 COMMENT '0未使用 1已锁定 2已使用 3已过期',
    order_sn    VARCHAR(32)          DEFAULT NULL,
    lock_time   DATETIME(3)          DEFAULT NULL,
    use_time    DATETIME(3)          DEFAULT NULL,
    create_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    update_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    KEY idx_member_status (member_id, status),
    KEY idx_coupon_member (coupon_id, member_id)
) ENGINE = InnoDB COMMENT ='会员优惠券';

CREATE TABLE seckill_activity
(
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    title       VARCHAR(100) NOT NULL,
    start_date  DATE         NOT NULL,
    end_date    DATE         NOT NULL,
    status      TINYINT      NOT NULL DEFAULT 0 COMMENT '0未发布 1已发布 2已下线',
    create_time DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    update_time DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id)
) ENGINE = InnoDB COMMENT ='秒杀活动';

CREATE TABLE seckill_session
(
    id          BIGINT      NOT NULL AUTO_INCREMENT,
    name        VARCHAR(64) NOT NULL,
    start_time  TIME        NOT NULL COMMENT '每日开始时间',
    end_time    TIME        NOT NULL COMMENT '每日结束时间',
    status      TINYINT     NOT NULL DEFAULT 1,
    create_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    update_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id)
) ENGINE = InnoDB COMMENT ='秒杀场次';

CREATE TABLE seckill_sku
(
    id            BIGINT         NOT NULL AUTO_INCREMENT,
    activity_id   BIGINT         NOT NULL,
    session_id    BIGINT         NOT NULL,
    product_id    BIGINT         NOT NULL,
    sku_id        BIGINT         NOT NULL,
    product_name  VARCHAR(200)   NOT NULL,
    product_pic   VARCHAR(500)            DEFAULT NULL,
    seckill_price DECIMAL(10, 2) NOT NULL,
    seckill_stock INT            NOT NULL COMMENT '活动库存，活动开始前预热到 Redis',
    per_limit     INT            NOT NULL DEFAULT 1 COMMENT '每人限购件数',
    sort          INT            NOT NULL DEFAULT 0,
    create_time   DATETIME(3)    NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    update_time   DATETIME(3)    NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_activity_session_sku (activity_id, session_id, sku_id),
    CONSTRAINT ck_seckill_stock CHECK (seckill_stock >= 0)
) ENGINE = InnoDB COMMENT ='秒杀商品';

-- 每个会员在同一秒杀商品上只能成功下单一次，数据库唯一索引是 Redis 去重之后的最终防线
CREATE TABLE seckill_order
(
    id             BIGINT      NOT NULL AUTO_INCREMENT,
    seckill_sku_id BIGINT      NOT NULL,
    member_id      BIGINT      NOT NULL,
    order_sn       VARCHAR(32) NOT NULL,
    quantity       INT         NOT NULL,
    create_time    DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_sku_member (seckill_sku_id, member_id),
    UNIQUE KEY uk_order_sn (order_sn)
) ENGINE = InnoDB COMMENT ='秒杀下单记录';

-- 本地消息表：与业务数据同事务写入，事务提交后投递，Publisher Confirm 回调置为已确认，定时任务重投超时未确认的消息
CREATE TABLE local_message
(
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    message_id      VARCHAR(64)  NOT NULL,
    biz_type        VARCHAR(32)  NOT NULL,
    biz_key         VARCHAR(64)  NOT NULL,
    exchange        VARCHAR(128) NOT NULL,
    routing_key     VARCHAR(128) NOT NULL,
    payload         JSON         NOT NULL,
    delay_ms        BIGINT       NOT NULL DEFAULT 0,
    status          TINYINT      NOT NULL DEFAULT 0 COMMENT '0待确认 1已确认 2超过重试上限',
    retry_count     INT          NOT NULL DEFAULT 0,
    next_retry_time DATETIME(3)  NOT NULL,
    last_error      VARCHAR(500)          DEFAULT NULL,
    create_time     DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    update_time     DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_message_id (message_id),
    KEY idx_status_retry (status, next_retry_time)
) ENGINE = InnoDB COMMENT ='本地消息表';
