package com.vayen.rdcm.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.vayen.rdcm.common.Result;
import com.vayen.rdcm.dto.LoginResponse;
import com.vayen.rdcm.entity.SysUser;
import com.vayen.rdcm.security.CurrentUser;
import com.vayen.rdcm.security.CurrentUserService;
import com.vayen.rdcm.security.LoginAttemptGuard;
import com.vayen.rdcm.security.RequestIpUtils;
import com.vayen.rdcm.security.RoleConstants;
import com.vayen.rdcm.service.AuthService;
import com.vayen.rdcm.service.SysUserService;
import com.vayen.rdcm.service.SystemLogService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

/**
 * 认证相关接口。
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final SysUserService userService;
    private final CurrentUserService currentUserService;
    private final SystemLogService systemLogService;
    private final LoginAttemptGuard loginAttemptGuard;

    @Data
    static class LoginRequest {
        private String username;
        private String password;
    }

    /**
     * 用户登录
     */
    @PostMapping("/login")
    public Result<LoginResponse> login(@RequestBody LoginRequest request, HttpServletRequest httpServletRequest) {
        String clientIp = RequestIpUtils.resolveClientIp(httpServletRequest);
        LocalDateTime lockedUntil = loginAttemptGuard.checkBlocked(clientIp);
        if (lockedUntil != null) {
            systemLogService.record(null, "认证", "登录", "POST /api/auth/login",
                    SystemLogService.STATUS_DENIED, "登录失败次数过多，IP 已被临时限制至 " + lockedUntil);
            return Result.error(429, "登录失败次数过多，请稍后再试");
        }

        try {
            LoginResponse response = authService.login(request.getUsername(), request.getPassword());
            loginAttemptGuard.recordSuccess(clientIp);
            CurrentUser currentUser = currentUserService.getCurrentUserOrNull();
            systemLogService.record(currentUser, "认证", "登录", "POST /api/auth/login",
                    SystemLogService.STATUS_SUCCESS, null);
            return Result.success(response);
        } catch (IllegalArgumentException ex) {
            LocalDateTime blockedAt = loginAttemptGuard.recordFailure(clientIp);
            String resultMessage = blockedAt == null
                    ? ex.getMessage()
                    : ex.getMessage() + "；该 IP 已临时限制到 " + blockedAt;
            systemLogService.record(null, "认证", "登录", "POST /api/auth/login",
                    SystemLogService.STATUS_FAIL, resultMessage);
            throw ex;
        }
    }

    /**
     * 用户退出登录
     */
    @PostMapping("/logout")
    public Result<Void> logout() {
        if (StpUtil.isLogin()) {
            CurrentUser currentUser = currentUserService.getCurrentUser();
            systemLogService.record(currentUser, "认证", "退出登录", "POST /api/auth/logout");
        }
        authService.logout();
        return Result.success();
    }

    /**
     * 获取当前登录用户信息
     */
    @GetMapping("/current")
    public Result<SysUser> currentUser() {
        if (!StpUtil.isLogin()) {
            return Result.error(401, "未登录或 token 已过期");
        }
        Long userId = StpUtil.getLoginIdAsLong();
        SysUser user = userService.getActiveById(userId);
        if (user == null) {
            return Result.error(401, "当前账号不可用");
        }
        user.setRole(RoleConstants.normalize(user.getRole()));
        user.setPasswordHash("");
        return Result.success(user);
    }
}
