package com.vayen.rdcm.service.impl;

import cn.dev33.satoken.secure.BCrypt;
import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.vayen.rdcm.dto.LoginResponse;
import com.vayen.rdcm.entity.SysUser;
import com.vayen.rdcm.mapper.SysUserMapper;
import com.vayen.rdcm.service.AuthService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@AllArgsConstructor
public class AuthServiceImpl extends ServiceImpl<SysUserMapper, SysUser> implements AuthService  {

    private final SysUserMapper sysUserMapper ;

    @Override
    public LoginResponse login(String username, String password) {
        QueryWrapper<SysUser> wrapper = new QueryWrapper<>();
        wrapper.eq("username", username)
                .eq("is_active", true);
        SysUser sysUser = sysUserMapper.selectOne(wrapper);
        LoginResponse loginResponse = new LoginResponse();

        // password 加密 BCrypt
        //String hash_pw = BCrypt.hashpw(password);

        if(BCrypt.checkpw(password,sysUser.getPasswordHash())){
            // 登录密码验证成功
            if(sysUser.getIsActive()){
                // 账号正常使用
                StpUtil.login(sysUser.getId());
                String token = StpUtil.getTokenValue();
                StpUtil.getSession().set("role",sysUser.getRole());
                StpUtil.getSession().set("companyId", sysUser.getCompanyId());
                loginResponse.setToken(token);
            }else{
                throw new RuntimeException("账号已停用");
            }
        }else {
            throw new RuntimeException("账号或密码有误");
        }
        loginResponse.setUserId(sysUser.getId());
        loginResponse.setUsername(sysUser.getUsername());
        loginResponse.setName(sysUser.getName());
        loginResponse.setRole(sysUser.getRole());
        return loginResponse;
    }
}
