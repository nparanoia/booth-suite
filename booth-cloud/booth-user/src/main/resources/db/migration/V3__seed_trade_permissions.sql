-- 第四步交易后台权限。超级管理员登录时得到 *，不用再分配。

INSERT INTO admin_permission (id, parent_id, code, name, type, sort)
VALUES (3, 0, 'trade', '交易', 1, 3),
       (301, 3, 'trade:order:view', '查看订单', 2, 1),
       (302, 3, 'trade:order:edit', '处理订单', 2, 2),
       (303, 3, 'trade:coupon:view', '查看优惠券', 2, 3),
       (304, 3, 'trade:coupon:edit', '管理优惠券', 2, 4);
