package com.vayen.rdcm.config;

import cn.dev33.satoken.stp.StpInterface;
import cn.dev33.satoken.stp.StpUtil;
import com.vayen.rdcm.security.PermissionConstants;
import com.vayen.rdcm.security.RoleConstants;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class StpInterfaceImpl implements StpInterface {

    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        List<String> roles = getRoleList(loginId, loginType);
        if (roles.contains(RoleConstants.ADMIN)) {
            return List.of(
                    PermissionConstants.COMPANY_MANAGE,
                    PermissionConstants.PROJECT_VIEW,
                    PermissionConstants.PROJECT_EDIT,
                    PermissionConstants.SETTLEMENT_VIEW,
                    PermissionConstants.SETTLEMENT_CREATE,
                    PermissionConstants.SETTLEMENT_EDIT
            );
        }
        if (roles.contains(RoleConstants.BRANCH_ADMIN)) {
            return List.of(
                    PermissionConstants.PROJECT_VIEW,
                    PermissionConstants.PROJECT_EDIT,
                    PermissionConstants.SETTLEMENT_VIEW,
                    PermissionConstants.SETTLEMENT_CREATE,
                    PermissionConstants.SETTLEMENT_EDIT
            );
        }
        if (roles.contains(RoleConstants.USER)) {
            return List.of(
                    PermissionConstants.PROJECT_VIEW,
                    PermissionConstants.PROJECT_EDIT,
                    PermissionConstants.SETTLEMENT_VIEW,
                    PermissionConstants.SETTLEMENT_CREATE
            );
        }
        return List.of();
    }

    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        if (loginId == null) {
            return List.of();
        }
        String role = RoleConstants.normalize(StpUtil.getSessionByLoginId(loginId).getString("role"));
        if (role == null || role.isBlank()) {
            return new ArrayList<>();
        }
        return List.of(role);
    }
}
