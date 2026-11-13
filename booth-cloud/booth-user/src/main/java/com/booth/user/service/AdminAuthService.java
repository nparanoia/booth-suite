package com.booth.user.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.booth.common.core.exception.BizException;
import com.booth.common.satoken.StpKit;
import com.booth.user.config.PasswordConfig.CredentialChecker;
import com.booth.user.dto.LoginRequest;
import com.booth.user.entity.AdminLoginLog;
import com.booth.user.entity.AdminRole;
import com.booth.user.entity.AdminUser;
import com.booth.user.enums.AccountStatus;
import com.booth.user.error.UserErrorCode;
import com.booth.user.mapper.AdminLoginLogMapper;
import com.booth.user.mapper.AdminUserMapper;
import com.booth.user.vo.AdminProfileVO;
import com.booth.user.vo.TokenVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 登录不加事务：密码错误时抛出异常，失败日志也必须保留。
 */
@Service
@RequiredArgsConstructor
public class AdminAuthService {

    private final AdminUserMapper adminUserMapper;
    private final AdminLoginLogMapper loginLogMapper;
    private final AdminPermissionService permissionService;
    private final CredentialChecker credentialChecker;
    private final LoginRateLimiter loginRateLimiter;

    public TokenVO login(LoginRequest request, String ip, String userAgent) {
        loginRateLimiter.guard("admin", ip);
        AdminUser admin = adminUserMapper.selectOne(Wrappers.<AdminUser>lambdaQuery()
                .eq(AdminUser::getUsername, request.username()));
        boolean passwordOk = credentialChecker.matches(request.password(), admin == null ? null : admin.getPassword());
        if (admin == null) {
            loginRateLimiter.recordFail("admin", ip);
            throw new BizException(UserErrorCode.LOGIN_FAILED);
        }
        if (!passwordOk) {
            loginRateLimiter.recordFail("admin", ip);
            log(admin.getId(), ip, userAgent, false);
            throw new BizException(UserErrorCode.LOGIN_FAILED);
        }
        if (admin.getStatus() != AccountStatus.ENABLED) {
            loginRateLimiter.recordFail("admin", ip);
            log(admin.getId(), ip, userAgent, false);
            throw new BizException(UserErrorCode.ADMIN_DISABLED);
        }

        // 先写权限缓存再签发 Token：网关拿到 Token 时权限一定已就绪
        permissionService.cacheOnLogin(admin.getId());
        String token = StpKit.ADMIN.createLoginSession(admin.getId());
        adminUserMapper.update(Wrappers.<AdminUser>lambdaUpdate()
                .set(AdminUser::getLastLoginTime, LocalDateTime.now())
                .eq(AdminUser::getId, admin.getId()));
        log(admin.getId(), ip, userAgent, true);
        loginRateLimiter.clear("admin", ip);
        return new TokenVO(token, StpKit.ADMIN.getTokenTimeout(token));
    }

    public void logout(String token) {
        if (token != null) {
            StpKit.ADMIN.logoutByTokenValue(token);
        }
    }

    public AdminProfileVO me(Long adminId) {
        AdminUser admin = adminUserMapper.selectById(adminId);
        BizException.throwIf(admin == null, UserErrorCode.ADMIN_NOT_FOUND);
        List<AdminRole> roles = permissionService.enabledRoles(adminId);
        return new AdminProfileVO(admin.getId(), admin.getUsername(), admin.getNickname(), admin.getAvatar(),
                roles.stream().map(AdminRole::getCode).toList(),
                permissionService.permissionCodes(roles));
    }

    private void log(Long adminId, String ip, String userAgent, boolean success) {
        AdminLoginLog log = new AdminLoginLog();
        log.setAdminId(adminId);
        log.setIp(ip);
        log.setUserAgent(userAgent != null && userAgent.length() > 255 ? userAgent.substring(0, 255) : userAgent);
        log.setSuccess(success);
        loginLogMapper.insert(log);
    }
}
