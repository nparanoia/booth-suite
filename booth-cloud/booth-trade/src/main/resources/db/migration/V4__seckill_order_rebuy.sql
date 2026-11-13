-- 原唯一键 (seckill_sku_id, member_id) 会把已关闭的秒杀单也算进去，超时关单后会员无法再抢。
-- 有效单把 active_member 写成会员号；关闭时置空。唯一键只约束仍然有效的那一笔。
ALTER TABLE seckill_order
    ADD COLUMN status        TINYINT NOT NULL DEFAULT 0 COMMENT '0有效 1已关闭' AFTER quantity,
    ADD COLUMN active_member BIGINT           NULL     COMMENT '有效单等于 member_id，关闭后置空' AFTER status;

UPDATE seckill_order SET active_member = member_id WHERE status = 0;

ALTER TABLE seckill_order DROP INDEX uk_sku_member;
ALTER TABLE seckill_order ADD UNIQUE KEY uk_sku_active_member (seckill_sku_id, active_member);
