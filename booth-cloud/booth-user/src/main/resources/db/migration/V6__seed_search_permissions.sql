-- 第六步搜索索引权限。超级管理员登录时得到 *，不用再分配。
-- 编号用 V6：V4 是积分流水注释，V5 是秒杀权限。

INSERT INTO admin_permission (id, parent_id, code, name, type, sort)
VALUES (4, 0, 'search', '搜索', 1, 4),
       (401, 4, 'search:index:edit', '重建搜索索引', 2, 1);
