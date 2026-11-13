-- 第五步秒杀后台权限。超级管理员登录时得到 *，不用再分配。
-- 编号用 V5：V4 已经用来补充积分流水注释。

INSERT INTO admin_permission (id, parent_id, code, name, type, sort)
VALUES (305, 3, 'trade:seckill:view', '查看秒杀', 2, 5),
       (306, 3, 'trade:seckill:edit', '管理秒杀', 2, 6);
