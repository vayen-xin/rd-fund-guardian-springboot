package com.vayen.rdcm.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.vayen.rdcm.common.Result;
import com.vayen.rdcm.entity.SysUser;
import com.vayen.rdcm.service.SysUserService;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

/**
 * 认证相关接口（简化版）
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private SysUserService userService;
    
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
    @Data
    static class LoginResponse {
        private Long userId;
        private String username;
        private String name;
        private String role;
        private String token;
    }
    
    /**
     * 用户登录（简化：明文比对）
     */
    @PostMapping("/login")
    public Result<LoginResponse> login(@RequestBody LoginRequest request) {
        SysUser user = userService.findByUsername(request.getUsername());
        if (user == null || !user.getPasswordHash().equals(request.getPassword())) {
            return Result.error("用户名或密码错误");
        }
        
        // 生成token（SA-Token）
        StpUtil.login(user.getId());
        String token = StpUtil.getTokenValue();
        
        LoginResponse response = new LoginResponse();
        response.setUserId(user.getId());
        response.setUsername(user.getUsername());
        response.setName(user.getName());
        response.setRole(user.getRole());
        response.setToken(token);
        
        return Result.success(response);
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
        return Result.success(user);
    }
}
