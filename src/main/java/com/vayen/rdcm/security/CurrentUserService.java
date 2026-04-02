package com.vayen.rdcm.security;

import cn.dev33.satoken.stp.StpUtil;
import com.vayen.rdcm.entity.SysUser;
import com.vayen.rdcm.service.SysUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CurrentUserService {

    private final SysUserService sysUserService;

    /**
     * 统一解析当前登录用户。
     * 后续 controller/service 只依赖这个方法，不再自己去 session 里取 role/companyId。
     */
    public CurrentUser getCurrentUser() {
        Long userId = StpUtil.getLoginIdAsLong();
        SysUser user = sysUserService.getActiveById(userId);
        if (user == null) {
            throw new IllegalArgumentException("当前登录用户不存在或已停用");
        }
        return CurrentUser.builder()
                .id(user.getId())
                .companyId(user.getCompanyId())
                .username(user.getUsername())
                .name(user.getName())
                .role(RoleConstants.normalize(user.getRole()))
                .active(Boolean.TRUE.equals(user.getIsActive()))
                .build();
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
     * 校验当前用户是否有权访问指定公司的数据。
     * admin 不受 company_id 限制，其他角色只能访问自己公司。
     */
    public void assertCompanyAccess(Long companyId) {
        if (companyId == null || isAdmin()) {
            return;
        }
        Long currentCompanyId = getCurrentCompanyId();
        if (currentCompanyId == null || !currentCompanyId.equals(companyId)) {
            throw new IllegalArgumentException("禁止访问其他公司的数据");
        }
    }
}
