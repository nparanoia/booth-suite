package com.booth.user.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.booth.common.satoken.AdminPermsCache;
import com.booth.common.satoken.StpKit;
import com.booth.user.entity.AdminPermission;
import com.booth.user.entity.AdminRole;
import com.booth.user.entity.AdminRolePermission;
import com.booth.user.entity.AdminUserRole;
import com.booth.user.enums.AccountStatus;
import com.booth.user.mapper.AdminPermissionMapper;
import com.booth.user.mapper.AdminRoleMapper;
import com.booth.user.mapper.AdminRolePermissionMapper;
import com.booth.user.mapper.AdminUserRoleMapper;
import com.booth.user.vo.PermissionTreeVO;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Duration;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 计算管理员的有效权限码，并维护网关读取的 Redis 缓存。
 * <ul>
 *     <li>只统计启用状态的角色；拥有 SUPER_ADMIN 角色时直接返回通配符 *</li>
 *     <li>登录时写入缓存，有效期与 Token 一致</li>
 *     <li>角色或权限变更后，在事务提交后刷新在线管理员的缓存，修改立即生效且不会把未提交的数据写进缓存</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class AdminPermissionService {

    public static final String SUPER_ADMIN = "SUPER_ADMIN";

    private final AdminUserRoleMapper userRoleMapper;
    private final AdminRoleMapper roleMapper;
    private final AdminRolePermissionMapper rolePermissionMapper;
    private final AdminPermissionMapper permissionMapper;
    private final StringRedisTemplate redis;

    public List<AdminRole> enabledRoles(Long adminId) {
        List<Long> roleIds = userRoleMapper.selectList(Wrappers.<AdminUserRole>lambdaQuery()
                        .eq(AdminUserRole::getAdminId, adminId))
                .stream().map(AdminUserRole::getRoleId).toList();
        if (roleIds.isEmpty()) {
            return List.of();
        }
        return roleMapper.selectList(Wrappers.<AdminRole>lambdaQuery()
                .in(AdminRole::getId, roleIds)
                .eq(AdminRole::getStatus, AccountStatus.ENABLED)
                .orderByAsc(AdminRole::getSort));
    }

    public List<String> permissionCodes(List<AdminRole> roles) {
        if (roles.stream().anyMatch(role -> SUPER_ADMIN.equals(role.getCode()))) {
            return List.of(AdminPermsCache.WILDCARD);
        }
        if (roles.isEmpty()) {
            return List.of();
        }
        List<Long> permissionIds = rolePermissionMapper.selectList(Wrappers.<AdminRolePermission>lambdaQuery()
                        .in(AdminRolePermission::getRoleId, roles.stream().map(AdminRole::getId).toList()))
                .stream().map(AdminRolePermission::getPermissionId).distinct().toList();
        if (permissionIds.isEmpty()) {
            return List.of();
        }
        return permissionMapper.selectByIds(permissionIds).stream()
                .map(AdminPermission::getCode).sorted().toList();
    }

    public void cacheOnLogin(Long adminId) {
        long timeout = StpKit.ADMIN.getConfigOrGlobal().getTimeout();
        redis.opsForValue().set(AdminPermsCache.key(adminId), joinedCodes(adminId), Duration.ofSeconds(timeout));
    }

    /**
     * 有事务时在提交后执行，没有事务时立即执行。
     */
    public void refreshAfterCommit(Collection<Long> adminIds) {
        if (adminIds.isEmpty()) {
            return;
        }
        List<Long> ids = List.copyOf(adminIds);
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            ids.forEach(this::refreshIfOnline);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                ids.forEach(AdminPermissionService.this::refreshIfOnline);
            }
        });
    }

    public List<Long> adminIdsOfRole(Long roleId) {
        return userRoleMapper.selectList(Wrappers.<AdminUserRole>lambdaQuery()
                        .eq(AdminUserRole::getRoleId, roleId))
                .stream().map(AdminUserRole::getAdminId).toList();
    }

    public void evict(Long adminId) {
        redis.delete(AdminPermsCache.key(adminId));
    }

    public List<PermissionTreeVO> tree() {
        List<AdminPermission> all = permissionMapper.selectList(Wrappers.<AdminPermission>lambdaQuery()
                .orderByAsc(AdminPermission::getSort, AdminPermission::getId));
        Map<Long, List<AdminPermission>> byParent = all.stream()
                .collect(Collectors.groupingBy(AdminPermission::getParentId));
        return buildChildren(0L, byParent);
    }

    private List<PermissionTreeVO> buildChildren(Long parentId, Map<Long, List<AdminPermission>> byParent) {
        return byParent.getOrDefault(parentId, List.of()).stream()
                .sorted(Comparator.comparing(AdminPermission::getSort))
                .map(p -> new PermissionTreeVO(p.getId(), p.getCode(), p.getName(), p.getType(),
                        buildChildren(p.getId(), byParent)))
                .toList();
    }

    /**
     * 只刷新在线（缓存存在）的管理员，并保留剩余有效期；离线的管理员下次登录时重新计算。
     */
    private void refreshIfOnline(Long adminId) {
        String key = AdminPermsCache.key(adminId);
        Long ttl = redis.getExpire(key, TimeUnit.SECONDS);
        if (ttl == null || ttl <= 0) {
            return;
        }
        redis.opsForValue().set(key, joinedCodes(adminId), Duration.ofSeconds(ttl));
    }

    private String joinedCodes(Long adminId) {
        return String.join(",", permissionCodes(enabledRoles(adminId)));
    }
}
