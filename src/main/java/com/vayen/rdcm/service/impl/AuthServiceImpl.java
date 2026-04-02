package com.vayen.rdcm.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.vayen.rdcm.dto.LoginResponse;
import com.vayen.rdcm.entity.SysUser;
import com.vayen.rdcm.mapper.SysUserMapper;
import com.vayen.rdcm.security.RoleConstants;
import com.vayen.rdcm.service.AuthService;
import com.vayen.rdcm.service.SysUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class AuthServiceImpl extends ServiceImpl<SysUserMapper, SysUser> implements AuthService {

    private final SysUserService sysUserService;

    /**
     * 统一登录方法。
     * 关键步骤：
     * 1. 查启用中的账号
     * 2. 用 BCrypt 校验密码
     * 3. 写入 Sa-Token 登录态和角色/公司上下文
     */
    @Override
    public LoginResponse login(String username, String password) {
        SysUser sysUser = sysUserService.getActiveByUsername(username);
        if (sysUser == null || !sysUserService.passwordMatches(password, sysUser.getPasswordHash())) {
            throw new IllegalArgumentException("账号或密码有误");
        }

        // 登录成功后，把角色和 companyId 写进 session，后面的鉴权和数据隔离都靠它。
        StpUtil.login(sysUser.getId());
        StpUtil.getSession().set("role", RoleConstants.normalize(sysUser.getRole()));
        StpUtil.getSession().set("companyId", sysUser.getCompanyId());

        LoginResponse loginResponse = new LoginResponse();
        loginResponse.setUserId(sysUser.getId());
        loginResponse.setUsername(sysUser.getUsername());
        loginResponse.setName(sysUser.getName());
        loginResponse.setRole(RoleConstants.normalize(sysUser.getRole()));
        loginResponse.setToken(StpUtil.getTokenValue());
        return loginResponse;
    }
}
