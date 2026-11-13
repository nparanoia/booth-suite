-- ORDER_ABORT：退回发生在扣减提交之前，用来拦住晚到的扣减。金额为 0，不改余额。
ALTER TABLE member_integration_log
    MODIFY biz_type VARCHAR(32) NOT NULL COMMENT 'ORDER_DEDUCT / ORDER_REFUND / ORDER_REWARD / ORDER_ABORT / ADMIN_ADJUST';
