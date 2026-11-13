package com.booth.user.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.booth.common.core.exception.BizException;
import com.booth.user.convert.AdminConvert;
import com.booth.user.dto.RoleCreateRequest;
import com.booth.user.dto.RoleUpdateRequest;
import com.booth.user.entity.AdminPermission;
import com.booth.user.entity.AdminRole;
import com.booth.user.entity.AdminRolePermission;
import com.booth.user.enums.AccountStatus;
import com.booth.user.error.UserErrorCode;
import com.booth.user.mapper.AdminPermissionMapper;
import com.booth.user.mapper.AdminRoleMapper;
import com.booth.user.mapper.AdminRolePermissionMapper;
import com.booth.user.vo.RoleVO;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AdminRoleService {

    private final AdminRoleMapper roleMapper;
    private final AdminRolePermissionMapper rolePermissionMapper;
    private final AdminPermissionMapper permissionMapper;
    private final AdminPermissionService permissionService;
    private final AdminConvert adminConvert;

    public List<RoleVO> list() {
        return roleMapper.selectList(Wrappers.<AdminRole>lambdaQuery()
                        .orderByAsc(AdminRole::getSort, AdminRole::getId))
                .stream().map(adminConvert::toVO).toList();
    }

    public Long create(RoleCreateRequest request) {
        BizException.throwIf(roleMapper.exists(Wrappers.<AdminRole>lambdaQuery()
                .eq(AdminRole::getCode, request.code())), UserErrorCode.ROLE_CODE_EXISTS);
        AdminRole role = adminConvert.toEntity(request);
        role.setSort(Objects.requireNonNullElse(request.sort(), 0));
        role.setStatus(AccountStatus.ENABLED);
        try {
            roleMapper.insert(role);
        } catch (DuplicateKeyException e) {
            throw new BizException(UserErrorCode.ROLE_CODE_EXISTS);
        }
        return role.getId();
    }

    /**
     * 角色启用状态影响拥有它的管理员的有效权限，状态变化时刷新这些管理员的缓存。
     */
    @Transactional(rollbackFor = Exception.class)
    public void update(Long roleId, RoleUpdateRequest request) {
        AdminRole role = require(roleId);
        BizException.throwIf(isSuperAdmin(role) && request.status() == AccountStatus.DISABLED,
                UserErrorCode.SUPER_ROLE_READONLY);
        boolean statusChanged = role.getStatus() != request.status();
        adminConvert.update(role, request);
        roleMapper.updateById(role);
        if (statusChanged) {
            permissionService.refreshAfterCommit(permissionService.adminIdsOfRole(roleId));
        }
    }

    public List<Long> permissionIds(Long roleId) {
        require(roleId);
        return rolePermissionMapper.selectList(Wrappers.<AdminRolePermission>lambdaQuery()
                        .eq(AdminRolePermission::getRoleId, roleId))
                .stream().map(AdminRolePermission::getPermissionId).toList();
    }

    @Transactional(rollbackFor = Exception.class)
    public void assignPermissions(Long roleId, Set<Long> permissionIds) {
        BizException.throwIf(roleMapper.lockById(roleId) == null, UserErrorCode.ROLE_NOT_FOUND);
        BizException.throwIf(isSuperAdmin(require(roleId)), UserErrorCode.SUPER_ROLE_READONLY);
        if (!permissionIds.isEmpty()) {
            long found = permissionMapper.selectCount(Wrappers.<AdminPermission>lambdaQuery()
                    .in(AdminPermission::getId, permissionIds));
            BizException.throwIf(found != permissionIds.size(), UserErrorCode.PERMISSION_NOT_FOUND);
        }
        rolePermissionMapper.delete(Wrappers.<AdminRolePermission>lambdaQuery()
                .eq(AdminRolePermission::getRoleId, roleId));
        if (!permissionIds.isEmpty()) {
            rolePermissionMapper.insert(permissionIds.stream().map(permissionId -> {
                AdminRolePermission relation = new AdminRolePermission();
                relation.setRoleId(roleId);
                relation.setPermissionId(permissionId);
                return relation;
            }).toList());
        }
        permissionService.refreshAfterCommit(permissionService.adminIdsOfRole(roleId));
    }

    private AdminRole require(Long roleId) {
        AdminRole role = roleMapper.selectById(roleId);
        BizException.throwIf(role == null, UserErrorCode.ROLE_NOT_FOUND);
        return role;
    }

    private static boolean isSuperAdmin(AdminRole role) {
        return AdminPermissionService.SUPER_ADMIN.equals(role.getCode());
    }
}
