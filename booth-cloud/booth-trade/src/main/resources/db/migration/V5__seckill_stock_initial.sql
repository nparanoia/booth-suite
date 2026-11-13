-- 对账要知道创建时的活动库存。只拿当前剩余库存减不出已售件数，Redis 过期后更没有初始值。
-- 已有数据：剩余库存加上仍有效的秒杀件数，等于还没归还前的初始库存。
ALTER TABLE seckill_sku
    ADD COLUMN seckill_stock_initial INT NULL COMMENT '创建时的活动库存，对账用' AFTER seckill_stock;

UPDATE seckill_sku s
    LEFT JOIN (
        SELECT seckill_sku_id, SUM(quantity) AS qty
        FROM seckill_order
        WHERE status = 0
        GROUP BY seckill_sku_id
    ) o ON o.seckill_sku_id = s.id
SET s.seckill_stock_initial = s.seckill_stock + IFNULL(o.qty, 0);

ALTER TABLE seckill_sku
    MODIFY seckill_stock_initial INT NOT NULL;
