package com.vayen.rdcm.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.vayen.rdcm.common.Result;
import com.vayen.rdcm.dto.LoginResponse;
import com.vayen.rdcm.entity.SysUser;
import com.vayen.rdcm.service.AuthService;
import com.vayen.rdcm.service.SysUserService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;



/**
 * 认证相关接口（简化版）
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    private final SysUserService userService;
    
    /**
     * 登录请求体
     */
    @Data
    static class LoginRequest {
        private String username;
        private String password;
    }
    
    /**
     * 登录响应体
     */
//    @Data
//    static class LoginResponse {
//        private Long userId;
//        private String username;
//        private String name;
//        private String role;
//        private String token;
//    }
    
    /**
     * 用户登录
     */
    @PostMapping("/login")
    public Result<LoginResponse> login(@RequestBody LoginRequest request) {

        LoginResponse loginResponse = authService.login(request.username , request.password);
//        SysUser user = userService.findByUsername(request.getUsername());
//        if (user == null || !user.getPasswordHash().equals(request.getPassword())) {
//            return Result.error("用户名或密码错误");
//        }
//
//        // 生成token（SA-Token）
//        StpUtil.login(user.getId());
//        String token = StpUtil.getTokenValue();
//
//        LoginResponse response = new LoginResponse();
//        response.setUserId(user.getId());
//        response.setUsername(user.getUsername());
//        response.setName(user.getName());
//        response.setRole(user.getRole());
//        response.setToken(token);
        return Result.success(loginResponse);
    }
    
    /**
     * 登出
     */
    @PostMapping("/logout")
    public Result<Void> logout() {
        if (StpUtil.isLogin()) {
            StpUtil.logout();
        }
        return Result.success();
    }
    
    /**
     * 获取当前用户信息
     */
    @GetMapping("/current")
    public Result<SysUser> currentUser() {
        if (!StpUtil.isLogin()) {
            return Result.error("未登录");
        }
        Long userId = StpUtil.getLoginIdAsLong();
        SysUser user = userService.getById(userId);
        // 密码保密，虽然不能解密先不传吧
        user.setPasswordHash("");
        return Result.success(user);
    }
}
