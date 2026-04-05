package com.vayen.rdcm.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.vayen.rdcm.dto.LoginResponse;
import com.vayen.rdcm.entity.SysUser;
import com.vayen.rdcm.mapper.SysUserMapper;
import com.vayen.rdcm.security.CurrentUserService;
import com.vayen.rdcm.security.RoleConstants;
import com.vayen.rdcm.service.AuthService;
import com.vayen.rdcm.service.SysUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 登录认证服务。
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class AuthServiceImpl extends ServiceImpl<SysUserMapper, SysUser> implements AuthService {

    private final SysUserService sysUserService;
    private final CurrentUserService currentUserService;

    /**
     * 统一登录方法。
     */
    @Override
    public LoginResponse login(String username, String password) {
        SysUser sysUser = sysUserService.getActiveByUsername(username);
        if (sysUser == null || !sysUserService.passwordMatches(password, sysUser.getPasswordHash())) {
            throw new IllegalArgumentException("账号或密码有误");
        }

        StpUtil.login(sysUser.getId());
        currentUserService.cacheCurrentUser(sysUser);

        LoginResponse loginResponse = new LoginResponse();
        loginResponse.setUserId(sysUser.getId());
        loginResponse.setUsername(sysUser.getUsername());
        loginResponse.setName(sysUser.getName());
        loginResponse.setRole(RoleConstants.normalize(sysUser.getRole()));
        loginResponse.setToken(StpUtil.getTokenValue());
        return loginResponse;
    }

    @Override
    public void logout() {
        if (StpUtil.isLogin()) {
            StpUtil.logout();
        }
    }
}
