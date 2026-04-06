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
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

/**
 * 认证相关接口。
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
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
     * 用户登录。
     */
    @PostMapping("/login")
    public Result<LoginResponse> login(@RequestBody LoginRequest request, HttpServletRequest httpServletRequest) {
        String clientIp = RequestIpUtils.resolveClientIp(httpServletRequest);
        LocalDateTime lockedUntil = loginAttemptGuard.checkBlocked(clientIp);
        if (lockedUntil != null) {
            log.warn("IP {} 登录失败次数过多，已限制到 {}", clientIp, lockedUntil);
            systemLogService.record(null, "认证", "登录", "POST /api/auth/login", SystemLogService.STATUS_DENIED, "IP 已被临时限制到 " + lockedUntil);
            return Result.error(429, "登录失败次数过多，请稍后再试");
        }

        try {
            LoginResponse response = authService.login(request.getUsername(), request.getPassword());
            loginAttemptGuard.recordSuccess(clientIp);
            CurrentUser currentUser = currentUserService.getCurrentUserOrNull();
            log.info("用户 {} 登录成功，角色={}，公司ID={}", request.getUsername(), currentUser == null ? "-" : currentUser.getRole(), currentUser == null ? "-" : currentUser.getCompanyId());
            systemLogService.record(currentUser, "认证", "登录", "POST /api/auth/login", SystemLogService.STATUS_SUCCESS, null);
            return Result.success(response);
        } catch (IllegalArgumentException ex) {
            LocalDateTime blockedAt = loginAttemptGuard.recordFailure(clientIp);
            String resultMessage = blockedAt == null ? "账号或密码错误" : "账号或密码错误；该 IP 已限制到 " + blockedAt;
            log.warn("用户 {} 登录失败，IP={}，原因={}", request.getUsername(), clientIp, resultMessage);
            systemLogService.record(null, "认证", "登录", "POST /api/auth/login", SystemLogService.STATUS_FAIL, resultMessage);
            return Result.error(blockedAt == null ? 400 : 429, blockedAt == null ? "账号或密码错误" : "登录失败次数过多，请稍后再试");
        }
    }

    /**
     * 用户退出登录。
     */
    @PostMapping("/logout")
    public Result<Void> logout() {
        if (StpUtil.isLogin()) {
            CurrentUser currentUser = currentUserService.getCurrentUser();
            log.info("用户 {} 退出登录", currentUser.getUsername());
            systemLogService.record(currentUser, "认证", "退出登录", "POST /api/auth/logout");
        }
        authService.logout();
        return Result.success();
    }

    /**
     * 获取当前登录用户信息。
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
