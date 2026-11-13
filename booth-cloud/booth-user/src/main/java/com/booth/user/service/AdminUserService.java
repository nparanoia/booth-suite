package com.booth.user.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.booth.common.core.exception.BizException;
import com.booth.common.core.result.PageResult;
import com.booth.common.mybatis.page.PageResults;
import com.booth.common.satoken.StpKit;
import com.booth.user.convert.AdminConvert;
import com.booth.user.dto.AdminCreateRequest;
import com.booth.user.dto.AdminQuery;
import com.booth.user.entity.AdminRole;
import com.booth.user.entity.AdminUser;
import com.booth.user.entity.AdminUserRole;
import com.booth.user.enums.AccountStatus;
import com.booth.user.error.UserErrorCode;
import com.booth.user.mapper.AdminRoleMapper;
import com.booth.user.mapper.AdminUserMapper;
import com.booth.user.mapper.AdminUserRoleMapper;
import com.booth.user.vo.AdminUserVO;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminUserService {

    private final AdminUserMapper adminUserMapper;
    private final AdminUserRoleMapper userRoleMapper;
    private final AdminRoleMapper roleMapper;
    private final AdminPermissionService permissionService;
    private final AdminConvert adminConvert;
    private final PasswordEncoder passwordEncoder;

    public PageResult<AdminUserVO> page(AdminQuery query) {
        String keyword = query.getKeyword();
        Page<AdminUser> page = adminUserMapper.selectPage(query.toPage(), Wrappers.<AdminUser>lambdaQuery()
                .and(StringUtils.hasText(keyword), w -> w.like(AdminUser::getUsername, keyword)
                        .or().like(AdminUser::getNickname, keyword))
                .eq(query.getStatus() != null, AdminUser::getStatus, query.getStatus())
                .orderByDesc(AdminUser::getId));
        Map<Long, List<Long>> roleIdsByAdmin = page.getRecords().isEmpty() ? Map.of()
                : userRoleMapper.selectList(Wrappers.<AdminUserRole>lambdaQuery()
                        .in(AdminUserRole::getAdminId, page.getRecords().stream().map(AdminUser::getId).toList()))
                .stream().collect(Collectors.groupingBy(AdminUserRole::getAdminId,
                        Collectors.mapping(AdminUserRole::getRoleId, Collectors.toList())));
        return PageResults.of(page, admin -> adminConvert.toVO(admin, roleIdsByAdmin.getOrDefault(admin.getId(), List.of())));
    }

    public Long create(AdminCreateRequest request) {
        BizException.throwIf(adminUserMapper.exists(Wrappers.<AdminUser>lambdaQuery()
                .eq(AdminUser::getUsername, request.username())), UserErrorCode.ADMIN_USERNAME_EXISTS);
        AdminUser admin = new AdminUser();
        admin.setUsername(request.username());
        admin.setPassword(passwordEncoder.encode(request.password()));
        admin.setNickname(StringUtils.hasText(request.nickname()) ? request.nickname() : request.username());
        admin.setEmail(request.email());
        admin.setStatus(AccountStatus.ENABLED);
        try {
            adminUserMapper.insert(admin);
        } catch (DuplicateKeyException e) {
            throw new BizException(UserErrorCode.ADMIN_USERNAME_EXISTS);
        }
        return admin.getId();
    }

    /**
     * 禁用后立即踢下线并清除权限缓存，已签发的 Token 马上失效。
     */
    public void updateStatus(Long operatorId, Long adminId, AccountStatus status) {
        BizException.throwIf(adminId.equals(operatorId) && status == AccountStatus.DISABLED,
                UserErrorCode.CANNOT_DISABLE_SELF);
        BizException.throwIf(adminUserMapper.selectById(adminId) == null, UserErrorCode.ADMIN_NOT_FOUND);
        adminUserMapper.update(Wrappers.<AdminUser>lambdaUpdate()
                .set(AdminUser::getStatus, status)
                .eq(AdminUser::getId, adminId));
        if (status == AccountStatus.DISABLED) {
            StpKit.ADMIN.logout(adminId);
            permissionService.evict(adminId);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void assignRoles(Long adminId, Set<Long> roleIds) {
        BizException.throwIf(adminUserMapper.lockById(adminId) == null, UserErrorCode.ADMIN_NOT_FOUND);
        if (!roleIds.isEmpty()) {
            long found = roleMapper.selectCount(Wrappers.<AdminRole>lambdaQuery().in(AdminRole::getId, roleIds));
            BizException.throwIf(found != roleIds.size(), UserErrorCode.ROLE_NOT_FOUND);
        }
        userRoleMapper.delete(Wrappers.<AdminUserRole>lambdaQuery().eq(AdminUserRole::getAdminId, adminId));
        if (!roleIds.isEmpty()) {
            userRoleMapper.insert(roleIds.stream().map(roleId -> {
                AdminUserRole relation = new AdminUserRole();
                relation.setAdminId(adminId);
                relation.setRoleId(roleId);
                return relation;
            }).toList());
        }
        permissionService.refreshAfterCommit(List.of(adminId));
    }
}
