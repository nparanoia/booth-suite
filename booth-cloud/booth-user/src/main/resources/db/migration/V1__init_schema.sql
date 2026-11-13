-- 用户服务 booth_user：会员域 + 后台管理员 RBAC

CREATE TABLE member
(
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    username    VARCHAR(64)  NOT NULL COMMENT '用户名',
    password    VARCHAR(100) NOT NULL COMMENT 'BCrypt 哈希',
    nickname    VARCHAR(64)           DEFAULT NULL,
    phone       VARCHAR(20)  NOT NULL,
    avatar      VARCHAR(500)          DEFAULT NULL,
    gender      TINYINT      NOT NULL DEFAULT 0 COMMENT '0未知 1男 2女',
    birthday    DATE                  DEFAULT NULL,
    integration INT          NOT NULL DEFAULT 0 COMMENT '可用积分',
    status      TINYINT      NOT NULL DEFAULT 1 COMMENT '0禁用 1启用',
    create_time DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    update_time DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_username (username),
    UNIQUE KEY uk_phone (phone),
    CONSTRAINT ck_member_integration CHECK (integration >= 0)
) ENGINE = InnoDB COMMENT ='会员';

-- 积分只通过流水变更：(biz_type, biz_no) 唯一，保证下单抵扣、取消返还、完成赠送在重试下只生效一次
CREATE TABLE member_integration_log
(
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    member_id     BIGINT       NOT NULL,
    change_amount INT          NOT NULL COMMENT '正数增加，负数扣减',
    balance_after INT          NOT NULL COMMENT '变更后余额',
    biz_type      VARCHAR(32)  NOT NULL COMMENT 'ORDER_DEDUCT / ORDER_REFUND / ORDER_REWARD / ADMIN_ADJUST',
    biz_no        VARCHAR(64)  NOT NULL COMMENT '业务单号，如订单号',
    note          VARCHAR(200)          DEFAULT NULL,
    create_time   DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_biz (biz_type, biz_no),
    KEY idx_member_time (member_id, create_time)
) ENGINE = InnoDB COMMENT ='会员积分流水';

CREATE TABLE member_address
(
    id             BIGINT       NOT NULL AUTO_INCREMENT,
    member_id      BIGINT       NOT NULL,
    receiver_name  VARCHAR(64)  NOT NULL,
    receiver_phone VARCHAR(20)  NOT NULL,
    province       VARCHAR(32)  NOT NULL,
    city           VARCHAR(32)  NOT NULL,
    region         VARCHAR(32)  NOT NULL,
    detail_address VARCHAR(200) NOT NULL,
    post_code      VARCHAR(16)           DEFAULT NULL,
    is_default     TINYINT      NOT NULL DEFAULT 0,
    create_time    DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    update_time    DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    KEY idx_member_id (member_id)
) ENGINE = InnoDB COMMENT ='会员收货地址';

-- 原 MongoDB 中的商品收藏迁回 MySQL，冗余商品快照用于列表展示
CREATE TABLE member_collection
(
    id            BIGINT        NOT NULL AUTO_INCREMENT,
    member_id     BIGINT        NOT NULL,
    product_id    BIGINT        NOT NULL,
    product_name  VARCHAR(200)  NOT NULL,
    product_pic   VARCHAR(500)           DEFAULT NULL,
    product_price DECIMAL(10, 2)         DEFAULT NULL,
    create_time   DATETIME(3)   NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_member_product (member_id, product_id),
    KEY idx_member_time (member_id, create_time)
) ENGINE = InnoDB COMMENT ='会员商品收藏';

CREATE TABLE admin_user
(
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    username        VARCHAR(64)  NOT NULL,
    password        VARCHAR(100) NOT NULL COMMENT 'BCrypt 哈希',
    nickname        VARCHAR(64)           DEFAULT NULL,
    email           VARCHAR(100)          DEFAULT NULL,
    avatar          VARCHAR(500)          DEFAULT NULL,
    status          TINYINT      NOT NULL DEFAULT 1 COMMENT '0禁用 1启用',
    last_login_time DATETIME(3)           DEFAULT NULL,
    note            VARCHAR(200)          DEFAULT NULL,
    deleted         TINYINT      NOT NULL DEFAULT 0,
    create_time     DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    update_time     DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    -- 管理员账号删除后用户名不再复用，唯一索引不包含 deleted
    UNIQUE KEY uk_username (username)
) ENGINE = InnoDB COMMENT ='后台管理员';

CREATE TABLE admin_role
(
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    code        VARCHAR(64)  NOT NULL COMMENT '角色编码，如 SUPER_ADMIN',
    name        VARCHAR(64)  NOT NULL,
    description VARCHAR(200)          DEFAULT NULL,
    status      TINYINT      NOT NULL DEFAULT 1,
    sort        INT          NOT NULL DEFAULT 0,
    create_time DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    update_time DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_code (code)
) ENGINE = InnoDB COMMENT ='后台角色';

-- 权限码与接口上的 @SaCheckPermission 一一对应，取代原来按 URL 匹配的资源表
CREATE TABLE admin_permission
(
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    parent_id   BIGINT       NOT NULL DEFAULT 0,
    code        VARCHAR(100) NOT NULL COMMENT '权限码，如 product:spu:update',
    name        VARCHAR(64)  NOT NULL,
    type        TINYINT      NOT NULL COMMENT '1模块 2操作',
    sort        INT          NOT NULL DEFAULT 0,
    create_time DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    update_time DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_code (code),
    KEY idx_parent_id (parent_id)
) ENGINE = InnoDB COMMENT ='后台权限';

CREATE TABLE admin_user_role
(
    id          BIGINT      NOT NULL AUTO_INCREMENT,
    admin_id    BIGINT      NOT NULL,
    role_id     BIGINT      NOT NULL,
    create_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_admin_role (admin_id, role_id),
    KEY idx_role_id (role_id)
) ENGINE = InnoDB COMMENT ='管理员-角色';

CREATE TABLE admin_role_permission
(
    id            BIGINT      NOT NULL AUTO_INCREMENT,
    role_id       BIGINT      NOT NULL,
    permission_id BIGINT      NOT NULL,
    create_time   DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_role_permission (role_id, permission_id),
    KEY idx_permission_id (permission_id)
) ENGINE = InnoDB COMMENT ='角色-权限';

CREATE TABLE admin_login_log
(
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    admin_id    BIGINT       NOT NULL,
    ip          VARCHAR(64)           DEFAULT NULL,
    user_agent  VARCHAR(255)          DEFAULT NULL,
    success     TINYINT      NOT NULL COMMENT '0失败 1成功',
    create_time DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    KEY idx_admin_time (admin_id, create_time)
) ENGINE = InnoDB COMMENT ='管理员登录日志';
