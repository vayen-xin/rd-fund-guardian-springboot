package com.vayen.rdcm.security;

import cn.dev33.satoken.stp.StpUtil;
import com.vayen.rdcm.entity.SysUser;
import com.vayen.rdcm.service.SysUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 统一解析当前登录用户，并优先使用 session 中的用户上下文。
 */
@Service
@RequiredArgsConstructor
public class CurrentUserService {

    private static final String CURRENT_USER_SESSION_KEY = "currentUser";

    private final SysUserService sysUserService;

    /**
     * 获取当前登录用户，常用字段优先从 Sa-Token session 读取。
     */
    public CurrentUser getCurrentUser() {
        Object cached = StpUtil.getSession().get(CURRENT_USER_SESSION_KEY);
        if (cached instanceof CurrentUser currentUser) {
            return currentUser;
        }

        Long userId = StpUtil.getLoginIdAsLong();
        SysUser user = sysUserService.getActiveById(userId);
        if (user == null) {
            throw new IllegalArgumentException("当前登录用户不存在或已停用");
        }
        return cacheCurrentUser(user);
    }

    public CurrentUser getCurrentUserOrNull() {
        try {
            if (!StpUtil.isLogin()) {
                return null;
            }
            return getCurrentUser();
        } catch (Exception ignored) {
            return null;
        }
    }

    /**
     * 登录成功后将常用用户上下文写入 session，减少后续重复查库。
     */
    public CurrentUser cacheCurrentUser(SysUser user) {
        CurrentUser currentUser = buildCurrentUser(user);
        StpUtil.getSession().set(CURRENT_USER_SESSION_KEY, currentUser);
        StpUtil.getSession().set("role", currentUser.getRole());
        StpUtil.getSession().set("companyId", currentUser.getCompanyId());
        return currentUser;
    }

    /**
     * 刷新指定登录人的 session 上下文，适合改名、改角色等场景。
     */
    public void cacheCurrentUserByLoginId(SysUser user) {
        CurrentUser currentUser = buildCurrentUser(user);
        StpUtil.getSessionByLoginId(user.getId()).set(CURRENT_USER_SESSION_KEY, currentUser);
        StpUtil.getSessionByLoginId(user.getId()).set("role", currentUser.getRole());
        StpUtil.getSessionByLoginId(user.getId()).set("companyId", currentUser.getCompanyId());
    }

    public Long getCurrentUserId() {
        return getCurrentUser().getId();
    }

    public Long getCurrentCompanyId() {
        return getCurrentUser().getCompanyId();
    }

    public boolean isAdmin() {
        return getCurrentUser().isAdmin();
    }

    /**
     * 校验当前用户是否有权限访问指定公司的数据。
     */
    public void assertCompanyAccess(Long companyId) {
        CurrentUser currentUser = getCurrentUser();
        if (companyId == null || currentUser.isAdmin()) {
            return;
        }
        if (currentUser.getCompanyId() == null || !currentUser.getCompanyId().equals(companyId)) {
            throw new IllegalArgumentException("禁止访问其他公司的数据");
        }
    }

    private CurrentUser buildCurrentUser(SysUser user) {
        return CurrentUser.builder()
                .id(user.getId())
                .companyId(user.getCompanyId())
                .username(user.getUsername())
                .name(user.getName())
                .role(RoleConstants.normalize(user.getRole()))
                .active(Boolean.TRUE.equals(user.getIsActive()))
                .build();
    }
}
