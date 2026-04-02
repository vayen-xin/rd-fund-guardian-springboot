package com.vayen.rdcm.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.vayen.rdcm.common.Result;
import com.vayen.rdcm.dto.AccountResponse;
import com.vayen.rdcm.dto.PageResponse;
import com.vayen.rdcm.entity.SysUser;
import com.vayen.rdcm.security.CurrentUser;
import com.vayen.rdcm.security.CurrentUserService;
import com.vayen.rdcm.service.SysUserService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/accounts")
@RequiredArgsConstructor
public class AccountController {

    private final SysUserService sysUserService;
    private final CurrentUserService currentUserService;

    /**
     * 获取账号列表
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
     * 创建账号
     */
    @PostMapping
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
        AccountResponse response = new AccountResponse();
        response.setId(created.getId());
        response.setCompanyId(created.getCompanyId());
        response.setUsername(created.getUsername());
        response.setName(created.getName());
        response.setRole(com.vayen.rdcm.security.RoleConstants.normalize(created.getRole()));
        response.setEmail(created.getEmail());
        response.setPhone(created.getPhone());
        response.setStatus(Boolean.TRUE.equals(created.getIsActive()) ? "enabled" : "disabled");
        response.setCreatedAt(created.getCreatedAt());
        return Result.success(response);
    }

    /**
     * 修改账号状态
     */
    @PutMapping("/{id}/status")
    public Result<Void> updateStatus(@PathVariable Long id, @RequestBody UpdateStatusRequest request) {
        sysUserService.updateUserStatus(id, "enabled".equalsIgnoreCase(request.getStatus()), currentUserService.getCurrentUser());
        return Result.success();
    }

    /**
     * 修改本人资料
     */
    @PutMapping("/me/profile")
    public Result<Void> updateProfile(@RequestBody UpdateProfileRequest request) {
        sysUserService.updateMyProfile(currentUserService.getCurrentUserId(), request.getName(), request.getEmail(), request.getPhone());
        return Result.success();
    }

    /**
     * 修改本人密码
     */
    @PutMapping("/me/password")
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
