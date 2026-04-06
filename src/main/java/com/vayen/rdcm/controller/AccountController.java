package com.vayen.rdcm.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import cn.dev33.satoken.annotation.SaMode;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.vayen.rdcm.audit.AuditLog;
import com.vayen.rdcm.common.Result;
import com.vayen.rdcm.dto.AccountResponse;
import com.vayen.rdcm.dto.PageResponse;
import com.vayen.rdcm.entity.SysUser;
import com.vayen.rdcm.security.CurrentUser;
import com.vayen.rdcm.security.CurrentUserService;
import com.vayen.rdcm.security.RoleConstants;
import com.vayen.rdcm.service.SysUserService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 后台账号管理接口。
 */
@RestController
@RequestMapping("/api/v1/accounts")
@RequiredArgsConstructor
public class AccountController {

    private final SysUserService sysUserService;
    private final CurrentUserService currentUserService;

    /**
     * 获取账号列表。
     */
    @GetMapping
    public Result<PageResponse<AccountResponse>> getAccounts(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String role) {
        CurrentUser currentUser = currentUserService.getCurrentUser();
        Page<AccountResponse> result = sysUserService.getAccounts(currentUser, page, size, keyword, role);
        return Result.success(new PageResponse<>(result.getRecords(), result.getCurrent(), result.getSize(), result.getTotal()));
    }

    /**
     * 创建账号。
     */
    @PostMapping
    @SaCheckRole(value = {"admin", "branch_admin"}, mode = SaMode.OR)
    @AuditLog(module = "账号管理", action = "创建账号")
    public Result<AccountResponse> createAccount(@RequestBody CreateAccountRequest request) {
        CurrentUser currentUser = currentUserService.getCurrentUser();
        SysUser created = sysUserService.createUser(
                request.getUsername(),
                request.getName(),
                request.getRole(),
                request.getPassword(),
                request.getCompanyId(),
                currentUser
        );
        return Result.success(toAccountResponse(created));
    }

    /**
     * 修改账号状态。
     */
    @PutMapping("/{id}/status")
    @SaCheckRole(value = {"admin", "branch_admin"}, mode = SaMode.OR)
    @AuditLog(module = "账号管理", action = "修改账号状态")
    public Result<Void> updateStatus(@PathVariable Long id, @RequestBody UpdateStatusRequest request) {
        sysUserService.updateUserStatus(id, "enabled".equalsIgnoreCase(request.getStatus()), currentUserService.getCurrentUser());
        return Result.success();
    }

    /**
     * 修改本人资料。
     */
    @PutMapping("/me/profile")
    @AuditLog(module = "账号管理", action = "修改个人资料")
    public Result<Void> updateProfile(@RequestBody UpdateProfileRequest request) {
        Long currentUserId = currentUserService.getCurrentUserId();
        sysUserService.updateMyProfile(currentUserId, request.getName(), request.getEmail(), request.getPhone());
        SysUser updatedUser = sysUserService.getActiveById(currentUserId);
        if (updatedUser != null) {
            currentUserService.cacheCurrentUserByLoginId(updatedUser);
        }
        return Result.success();
    }

    /**
     * 修改本人密码。
     */
    @PutMapping("/me/password")
    @AuditLog(module = "账号管理", action = "修改密码")
    public Result<Void> updatePassword(@RequestBody UpdatePasswordRequest request) {
        boolean changed = sysUserService.changePassword(
                currentUserService.getCurrentUserId(),
                request.getOldPassword(),
                request.getNewPassword()
        );
        if (!changed) {
            return Result.error(400, "原密码不正确");
        }
        return Result.success();
    }

    private AccountResponse toAccountResponse(SysUser user) {
        AccountResponse response = new AccountResponse();
        response.setId(user.getId());
        response.setCompanyId(user.getCompanyId());
        response.setUsername(user.getUsername());
        response.setName(user.getName());
        response.setRole(RoleConstants.normalize(user.getRole()));
        response.setEmail(user.getEmail());
        response.setPhone(user.getPhone());
        response.setStatus(Boolean.TRUE.equals(user.getIsActive()) ? "enabled" : "disabled");
        response.setCreatedAt(user.getCreatedAt());
        return response;
    }

    @Data
    static class CreateAccountRequest {
        private String username;
        private String password;
        private String name;
        private String role;
        private Long companyId;
    }

    @Data
    static class UpdateStatusRequest {
        private String status;
    }

    @Data
    static class UpdateProfileRequest {
        private String name;
        private String email;
        private String phone;
    }

    @Data
    static class UpdatePasswordRequest {
        private String oldPassword;
        private String newPassword;
    }
}
