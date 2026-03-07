package com.vayen.rdcm.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.vayen.rdcm.common.Result;
import com.vayen.rdcm.dto.LoginRequest;
import com.vayen.rdcm.dto.LoginResponse;
import com.vayen.rdcm.entity.UserAccount;
import com.vayen.rdcm.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 认证控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserMapper userMapper;

    /**
     * 用户登录
     */
    @PostMapping("/login")
    public Result<LoginResponse> login(@RequestBody LoginRequest request) {
        log.info("用户登录：{}", request.getUsername());

        // 1. 查询用户
        UserAccount user = userMapper.findByUsername(request.getUsername());
        if (user == null) {
            return Result.error(401, "用户名或密码错误");
        }

        // 2. 验证密码（暂时简单比较，后续用 BCrypt）
        // TODO: 使用 BCrypt 加密验证
        if (!request.getPassword().equals(user.getPasswordHash())) {
            return Result.error(401, "用户名或密码错误");
        }

        // 3. 检查用户状态
        if (user.getStatus() != 1) {
            return Result.error(403, "账号已被禁用");
        }

        // 4. 登录，生成 token
        StpUtil.login(user.getId());

        // 5. 获取 token
        String token = StpUtil.getTokenValue();

        log.info("用户登录成功：{}, userId: {}", request.getUsername(), user.getId());

        return Result.success(new LoginResponse(user.getUsername(), token, user.getId()));
    }

    /**
     * 用户登出
     */
    @PostMapping("/logout")
    public Result<Void> logout() {
        StpUtil.logout();
        log.info("用户登出");
        return Result.success();
    }

    /**
     * 获取当前登录用户信息
     */
    @GetMapping("/current")
    public Result<UserAccount> getCurrentUser() {
        Long userId = StpUtil.getLoginIdAsLong();
        UserAccount user = userMapper.findById(userId);
        if (user == null) {
            return Result.error(404, "用户不存在");
        }
        // 不返回密码
        user.setPasswordHash(null);
        return Result.success(user);
    }
}
