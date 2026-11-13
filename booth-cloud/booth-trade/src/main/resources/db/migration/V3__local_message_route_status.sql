-- 状态 3：消息被退回，没有进队列。确认回调不能把这个状态改回已确认。
ALTER TABLE local_message
    MODIFY status TINYINT NOT NULL DEFAULT 0 COMMENT '0待确认 1已确认 2超过重试上限 3无法路由';
