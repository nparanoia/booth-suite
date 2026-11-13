-- 订单关闭后才收到支付成功通知时，支付记录使用状态 3，订单本身保持已关闭。
ALTER TABLE payment_record
    MODIFY COLUMN status TINYINT NOT NULL DEFAULT 0 COMMENT '0待支付 1支付成功 2已关闭 3关闭后到账待退款';