package com.vayen.rdcm.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.vayen.rdcm.dto.AccountResponse;
import com.vayen.rdcm.entity.SysUser;
import com.vayen.rdcm.mapper.SysUserMapper;
import com.vayen.rdcm.security.CurrentUser;
import com.vayen.rdcm.security.PasswordService;
import com.vayen.rdcm.security.RoleConstants;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class SysUserService extends ServiceImpl<SysUserMapper, SysUser> {

    private final SysUserMapper userMapper;
    private final PasswordService passwordService;

    public SysUser findByUsername(String username) {
        QueryWrapper<SysUser> wrapper = new QueryWrapper<>();
        wrapper.eq("username", username);
        return userMapper.selectOne(wrapper);
    }

    public SysUser getActiveById(Long userId) {
        QueryWrapper<SysUser> wrapper = new QueryWrapper<>();
        wrapper.eq("id", userId)
                .eq("is_active", true);
        return userMapper.selectOne(wrapper);
    }

    public SysUser getActiveByUsername(String username) {
        QueryWrapper<SysUser> wrapper = new QueryWrapper<>();
        wrapper.eq("username", username)
                .eq("is_active", true);
        return userMapper.selectOne(wrapper);
    }

    public boolean passwordMatches(String rawPassword, String encodedPassword) {
        return passwordService.matches(rawPassword, encodedPassword);
    }

    /**
     * 分页查询账号列表，并按当前登录人角色自动做数据隔离。
     */
    public Page<AccountResponse> getAccounts(CurrentUser currentUser, Integer page, Integer size, String keyword, String role) {
        QueryWrapper<SysUser> wrapper = new QueryWrapper<>();
        if (!currentUser.isAdmin()) {
            wrapper.eq("company_id", currentUser.getCompanyId());
        }
        if (keyword != null && !keyword.isBlank()) {
            wrapper.and(w -> w.like("username", keyword).or().like("name", keyword));
        }
        if (role != null && !role.isBlank()) {
            wrapper.eq("role", normalizePersistedRole(role));
        }
        wrapper.orderByDesc("created_at");
        Page<SysUser> result = userMapper.selectPage(new Page<>(page, size), wrapper);
        Page<AccountResponse> responsePage = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        responsePage.setRecords(result.getRecords().stream().map(this::toAccountResponse).toList());
        return responsePage;
    }

    /**
     * 创建后台账号。
     * branch_admin 只能在自己公司下创建 user，不能创建 admin 或 branch_admin。
     */
    public SysUser createUser(String username, String name, String role, String rawPassword, Long companyId, CurrentUser currentUser) {
        if (findByUsername(username) != null) {
            throw new IllegalArgumentException("用户名已存在");
        }
        String normalizedRole = RoleConstants.normalize(role);
        if (!currentUser.isAdmin()) {
            if (RoleConstants.ADMIN.equals(normalizedRole) || RoleConstants.BRANCH_ADMIN.equals(normalizedRole)) {
                throw new IllegalArgumentException("当前角色无权创建该账号");
            }
            companyId = currentUser.getCompanyId();
        }

        SysUser user = new SysUser();
        user.setUsername(username);
        user.setName(name);
        // 数据库枚举暂时仍是 employee，这里做一次持久化兼容。
        user.setRole(normalizePersistedRole(normalizedRole));
        user.setPasswordHash(passwordService.encode(rawPassword));
        user.setCompanyId(companyId);
        user.setIsActive(true);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.insert(user);
        return user;
    }

    /**
     * 修改账号启用状态。
     * 当前登录人不能停用自己。
     */
    public void updateUserStatus(Long targetUserId, boolean active, CurrentUser currentUser) {
        SysUser target = getAccessibleUser(targetUserId, currentUser);
        if (target.getId().equals(currentUser.getId()) && !active) {
            throw new IllegalArgumentException("不能停用当前登录账号");
        }
        target.setIsActive(active);
        target.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(target);
    }

    /**
     * 修改本人资料。
     */
    public void updateMyProfile(Long userId, String name, String email, String phone) {
        SysUser user = userMapper.selectById(userId);
        if (user == null) {
            throw new IllegalArgumentException("账号不存在");
        }
        user.setName(name);
        user.setEmail(email);
        user.setPhone(phone);
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(user);
    }

    /**
     * 修改本人密码。
     */
    public boolean changePassword(Long userId, String oldPassword, String newPassword) {
        SysUser user = userMapper.selectById(userId);
        if (user == null) {
            return false;
        }
        if (!passwordService.matches(oldPassword, user.getPasswordHash())) {
            return false;
        }
        user.setPasswordHash(passwordService.encode(newPassword));
        user.setUpdatedAt(LocalDateTime.now());
        return userMapper.updateById(user) > 0;
    }

    private SysUser getAccessibleUser(Long userId, CurrentUser currentUser) {
        SysUser user = userMapper.selectById(userId);
        if (user == null) {
            throw new IllegalArgumentException("账号不存在");
        }
        if (!currentUser.isAdmin() && !currentUser.getCompanyId().equals(user.getCompanyId())) {
            throw new IllegalArgumentException("禁止访问其他公司的账号");
        }
        return user;
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

    private String normalizePersistedRole(String role) {
        String normalized = RoleConstants.normalize(role);
        if (RoleConstants.USER.equals(normalized)) {
            return RoleConstants.LEGACY_EMPLOYEE;
        }
        return normalized;
    }
}
