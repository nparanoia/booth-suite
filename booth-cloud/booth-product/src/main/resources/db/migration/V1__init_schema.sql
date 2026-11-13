-- 商品服务 booth_product：商品基础数据、SKU 库存、首页内容

CREATE TABLE brand
(
    id           BIGINT       NOT NULL AUTO_INCREMENT,
    name         VARCHAR(64)  NOT NULL,
    first_letter CHAR(1)               DEFAULT NULL,
    logo         VARCHAR(500)          DEFAULT NULL,
    story        TEXT,
    sort         INT          NOT NULL DEFAULT 0,
    show_status  TINYINT      NOT NULL DEFAULT 1,
    create_time  DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    update_time  DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_name (name)
) ENGINE = InnoDB COMMENT ='品牌';

CREATE TABLE category
(
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    parent_id   BIGINT       NOT NULL DEFAULT 0 COMMENT '0 表示一级分类',
    name        VARCHAR(64)  NOT NULL,
    level       TINYINT      NOT NULL COMMENT '1一级 2二级',
    icon        VARCHAR(500)          DEFAULT NULL,
    keywords    VARCHAR(255)          DEFAULT NULL,
    sort        INT          NOT NULL DEFAULT 0,
    nav_status  TINYINT      NOT NULL DEFAULT 0 COMMENT '是否显示在导航栏',
    show_status TINYINT      NOT NULL DEFAULT 1,
    create_time DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    update_time DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    KEY idx_parent_sort (parent_id, sort)
) ENGINE = InnoDB COMMENT ='商品分类';

CREATE TABLE attr_group
(
    id          BIGINT      NOT NULL AUTO_INCREMENT,
    name        VARCHAR(64) NOT NULL,
    create_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    update_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id)
) ENGINE = InnoDB COMMENT ='属性分组（商品类型）';

CREATE TABLE attr
(
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    group_id    BIGINT       NOT NULL,
    name        VARCHAR(64)  NOT NULL,
    type        TINYINT      NOT NULL COMMENT '0销售规格 1展示参数',
    input_type  TINYINT      NOT NULL DEFAULT 0 COMMENT '0手工录入 1从列表选择',
    input_list  VARCHAR(500)          DEFAULT NULL COMMENT '可选值，逗号分隔',
    search_type TINYINT      NOT NULL DEFAULT 0 COMMENT '0不检索 1关键字 2范围',
    sort        INT          NOT NULL DEFAULT 0,
    create_time DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    update_time DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    KEY idx_group_id (group_id)
) ENGINE = InnoDB COMMENT ='商品属性';

CREATE TABLE category_attr
(
    id          BIGINT      NOT NULL AUTO_INCREMENT,
    category_id BIGINT      NOT NULL,
    attr_id     BIGINT      NOT NULL,
    create_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_category_attr (category_id, attr_id)
) ENGINE = InnoDB COMMENT ='分类下用于筛选的属性';

CREATE TABLE product
(
    id                BIGINT         NOT NULL AUTO_INCREMENT,
    category_id       BIGINT         NOT NULL,
    brand_id          BIGINT         NOT NULL,
    attr_group_id     BIGINT                  DEFAULT NULL,
    product_sn        VARCHAR(64)    NOT NULL COMMENT '货号',
    name              VARCHAR(200)   NOT NULL,
    sub_title         VARCHAR(255)            DEFAULT NULL,
    pic               VARCHAR(500)            DEFAULT NULL COMMENT '主图',
    album_pics        VARCHAR(2000)           DEFAULT NULL COMMENT '画册，逗号分隔',
    keywords          VARCHAR(255)            DEFAULT NULL,
    detail_html       MEDIUMTEXT,
    price             DECIMAL(10, 2) NOT NULL COMMENT '展示价，取 SKU 最低价',
    original_price    DECIMAL(10, 2)          DEFAULT NULL COMMENT '划线价',
    unit              VARCHAR(16)             DEFAULT NULL,
    weight            DECIMAL(10, 2)          DEFAULT NULL COMMENT '克',
    sale              INT            NOT NULL DEFAULT 0,
    sort              INT            NOT NULL DEFAULT 0,
    publish_status    TINYINT        NOT NULL DEFAULT 0 COMMENT '0下架 1上架',
    new_status        TINYINT        NOT NULL DEFAULT 0,
    recommend_status  TINYINT        NOT NULL DEFAULT 0,
    gift_integration  INT            NOT NULL DEFAULT 0 COMMENT '购买赠送积分',
    brand_name        VARCHAR(64)    NOT NULL COMMENT '冗余，供搜索与列表展示',
    category_name     VARCHAR(64)    NOT NULL COMMENT '冗余，供搜索与列表展示',
    deleted           TINYINT        NOT NULL DEFAULT 0,
    create_time       DATETIME(3)    NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    update_time       DATETIME(3)    NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    -- 货号不复用，删除后也保持唯一
    UNIQUE KEY uk_product_sn (product_sn),
    KEY idx_category_publish (category_id, publish_status),
    KEY idx_brand_id (brand_id)
) ENGINE = InnoDB COMMENT ='商品 SPU';

CREATE TABLE product_attr_value
(
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    product_id  BIGINT       NOT NULL,
    attr_id     BIGINT       NOT NULL,
    value       VARCHAR(500) NOT NULL COMMENT '参数单值；规格多值时逗号分隔',
    create_time DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    update_time DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_product_attr (product_id, attr_id)
) ENGINE = InnoDB COMMENT ='商品属性值';

-- 可售库存 = stock - lock_stock。锁定、扣减、释放全部使用带条件的原子 UPDATE，CHECK 约束兜底防止出现负库存
CREATE TABLE sku
(
    id          BIGINT         NOT NULL AUTO_INCREMENT,
    product_id  BIGINT         NOT NULL,
    sku_code    VARCHAR(64)    NOT NULL,
    spec_data   JSON           NOT NULL COMMENT '销售规格，如 [{"key":"颜色","value":"黑色"}]',
    price       DECIMAL(10, 2) NOT NULL,
    pic         VARCHAR(500)            DEFAULT NULL,
    stock       INT            NOT NULL DEFAULT 0 COMMENT '实物库存',
    lock_stock  INT            NOT NULL DEFAULT 0 COMMENT '已下单未支付的锁定库存',
    low_stock   INT            NOT NULL DEFAULT 0 COMMENT '预警值',
    sale        INT            NOT NULL DEFAULT 0,
    create_time DATETIME(3)    NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    update_time DATETIME(3)    NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_sku_code (sku_code),
    KEY idx_product_id (product_id),
    CONSTRAINT ck_sku_stock CHECK (stock >= 0 AND lock_stock >= 0 AND lock_stock <= stock)
) ENGINE = InnoDB COMMENT ='商品 SKU 与库存';

-- 库存锁定流水：一笔订单的一个 SKU 只有一条记录，状态只能 LOCKED -> DEDUCTED 或 LOCKED -> RELEASED，
-- 用于保证锁定 / 扣减 / 释放三个操作幂等，并作为定时对账的依据
CREATE TABLE stock_lock_record
(
    id          BIGINT      NOT NULL AUTO_INCREMENT,
    order_sn    VARCHAR(32) NOT NULL,
    sku_id      BIGINT      NOT NULL,
    quantity    INT         NOT NULL,
    status      TINYINT     NOT NULL COMMENT '1已锁定 2已扣减 3已释放',
    create_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    update_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_order_sku (order_sn, sku_id),
    KEY idx_status_time (status, create_time),
    CONSTRAINT ck_lock_quantity CHECK (quantity > 0)
) ENGINE = InnoDB COMMENT ='库存锁定流水';

CREATE TABLE home_banner
(
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    name        VARCHAR(100) NOT NULL,
    pic         VARCHAR(500) NOT NULL,
    url         VARCHAR(500)          DEFAULT NULL,
    sort        INT          NOT NULL DEFAULT 0,
    status      TINYINT      NOT NULL DEFAULT 0 COMMENT '0下线 1上线',
    start_time  DATETIME(3)           DEFAULT NULL,
    end_time    DATETIME(3)           DEFAULT NULL,
    create_time DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    update_time DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    KEY idx_status_sort (status, sort)
) ENGINE = InnoDB COMMENT ='首页轮播';

CREATE TABLE home_recommend
(
    id          BIGINT      NOT NULL AUTO_INCREMENT,
    product_id  BIGINT      NOT NULL,
    type        TINYINT     NOT NULL COMMENT '1新品推荐 2人气推荐',
    sort        INT         NOT NULL DEFAULT 0,
    status      TINYINT     NOT NULL DEFAULT 1,
    create_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    update_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_type_product (type, product_id)
) ENGINE = InnoDB COMMENT ='首页推荐商品';
