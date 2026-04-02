package com.vayen.rdcm.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.vayen.rdcm.common.Result;
import com.vayen.rdcm.dto.LoginResponse;
import com.vayen.rdcm.entity.SysUser;
import com.vayen.rdcm.security.RoleConstants;
import com.vayen.rdcm.service.AuthService;
import com.vayen.rdcm.service.SysUserService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final SysUserService userService;

    @Data
    static class LoginRequest {
        private String username;
        private String password;
    }

    /**
     * 用户登录
     */
    @PostMapping("/login")
    public Result<LoginResponse> login(@RequestBody LoginRequest request) {
        return Result.success(authService.login(request.getUsername(), request.getPassword()));
    }

    /**
     * 用户退出登录
     */
    @PostMapping("/logout")
    public Result<Void> logout() {
        if (StpUtil.isLogin()) {
            StpUtil.logout();
        }
        return Result.success();
    }

    /**
     * 获取当前登录用户信息
     */
    @GetMapping("/current")
    public Result<SysUser> currentUser() {
        if (!StpUtil.isLogin()) {
            return Result.error(401, "未登录");
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
