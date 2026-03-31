package com.vayen.rdcm.config;

import cn.dev33.satoken.stp.StpInterface;
import cn.dev33.satoken.stp.StpUtil;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;


public class StpInterfaceImpl implements StpInterface {
    @Override
    public List<String> getPermissionList(Object o, String s) {
        return List.of();
    }

    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        // 4. 从 Session 中取出你之前存入的角色信息
        Object roleObj = StpUtil.getSessionByLoginId(loginId).get("role");
        if (roleObj != null) {
            String rolesStr = roleObj.toString();
            // 假设 role 是一个字符串 "admin"
            return List.of(rolesStr);
        }
        return new ArrayList<>();
    }
}
