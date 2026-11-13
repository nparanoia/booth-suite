-- 后台权限码、内置角色与初始管理员。
-- 权限码与接口上的 @RequiresPermission 一一对应；新增后台接口时在新的迁移脚本里追加权限码。

INSERT INTO admin_permission (id, parent_id, code, name, type, sort)
VALUES (1, 0, 'product', '商品管理', 1, 1),
       (101, 1, 'product:brand:view', '查看品牌', 2, 1),
       (102, 1, 'product:brand:edit', '编辑品牌', 2, 2),
       (103, 1, 'product:category:view', '查看分类', 2, 3),
       (104, 1, 'product:category:edit', '编辑分类', 2, 4),
       (105, 1, 'product:attr:view', '查看属性', 2, 5),
       (106, 1, 'product:attr:edit', '编辑属性', 2, 6),
       (107, 1, 'product:product:view', '查看商品', 2, 7),
       (108, 1, 'product:product:edit', '编辑商品与 SKU', 2, 8),
       (109, 1, 'product:stock:edit', '调整库存', 2, 9),
       (2, 0, 'user', '用户与权限', 1, 2),
       (201, 2, 'user:member:view', '查看会员', 2, 1),
       (202, 2, 'user:member:edit', '启用禁用会员', 2, 2),
       (203, 2, 'user:admin:view', '查看管理员', 2, 3),
       (204, 2, 'user:admin:edit', '管理管理员账号', 2, 4),
       (205, 2, 'user:role:view', '查看角色与权限', 2, 5),
       (206, 2, 'user:role:edit', '管理角色与权限', 2, 6);

-- SUPER_ADMIN 不分配具体权限，登录时直接获得通配权限 *
INSERT INTO admin_role (id, code, name, description, status, sort)
VALUES (1, 'SUPER_ADMIN', '超级管理员', '拥有全部权限', 1, 1),
       (2, 'PRODUCT_OPERATOR', '商品运营', '维护商品资料，不能调整库存', 1, 2);

INSERT INTO admin_role_permission (role_id, permission_id)
VALUES (2, 101), (2, 102), (2, 103), (2, 104), (2, 105), (2, 106), (2, 107), (2, 108), (2, 201);

-- 初始账号 admin / admin123，部署后应立即修改密码
INSERT INTO admin_user (id, username, password, nickname, status)
VALUES (1, 'admin', '$2a$10$qm8UTumCpvg9pyX7lGiigOfUTeiRdGgS6k23jgFQGuK4dARi3TeP.', '超级管理员', 1);

INSERT INTO admin_user_role (admin_id, role_id)
VALUES (1, 1);
