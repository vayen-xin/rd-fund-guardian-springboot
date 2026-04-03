package com.vayen.rdcm.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.vayen.rdcm.dto.SystemLogPageResponse;
import com.vayen.rdcm.entity.SysUser;
import com.vayen.rdcm.entity.SystemLog;
import com.vayen.rdcm.mapper.SysUserMapper;
import com.vayen.rdcm.mapper.SystemLogMapper;
import com.vayen.rdcm.security.CurrentUser;
import com.vayen.rdcm.security.CurrentUserService;
import com.vayen.rdcm.service.SystemLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SystemLogServiceImpl implements SystemLogService {

    private final SystemLogMapper systemLogMapper;
    private final SysUserMapper sysUserMapper;
    private final CurrentUserService currentUserService;

    @Override
    public SystemLogPageResponse pageLogs(long page, long size, String operator, LocalDate startDate, LocalDate endDate) {
        CurrentUser currentUser = currentUserService.getCurrentUser();

        LambdaQueryWrapper<SystemLog> wrapper = new LambdaQueryWrapper<>();
        if (!currentUser.isAdmin()) {
            wrapper.eq(SystemLog::getCompanyId, currentUser.getCompanyId());
        }
        if (startDate != null) {
            wrapper.ge(SystemLog::getCreatedAt, startDate.atStartOfDay());
        }
        if (endDate != null) {
            wrapper.le(SystemLog::getCreatedAt, endDate.atTime(23, 59, 59));
        }

        List<Long> matchedUserIds = findMatchedUserIds(operator, currentUser);
        if (StringUtils.hasText(operator) && matchedUserIds.isEmpty()) {
            return emptyPage(page, size);
        }
        if (!matchedUserIds.isEmpty()) {
            wrapper.in(SystemLog::getUserId, matchedUserIds);
        }

        wrapper.orderByDesc(SystemLog::getCreatedAt);

        Page<SystemLog> resultPage = systemLogMapper.selectPage(new Page<>(page, size), wrapper);
        Map<Long, SysUser> userMap = loadUserMap(resultPage.getRecords());

        SystemLogPageResponse response = new SystemLogPageResponse();
        response.setCurrent(resultPage.getCurrent());
        response.setSize(resultPage.getSize());
        response.setTotal(resultPage.getTotal());
        response.setRecords(resultPage.getRecords().stream().map(log -> toItem(log, userMap.get(log.getUserId()))).toList());
        return response;
    }

    private List<Long> findMatchedUserIds(String operator, CurrentUser currentUser) {
        if (!StringUtils.hasText(operator)) {
            return Collections.emptyList();
        }

        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        if (!currentUser.isAdmin()) {
            wrapper.eq(SysUser::getCompanyId, currentUser.getCompanyId());
        }
        wrapper.and(query -> query.like(SysUser::getName, operator).or().like(SysUser::getUsername, operator));

        return sysUserMapper.selectList(wrapper).stream().map(SysUser::getId).toList();
    }

    private Map<Long, SysUser> loadUserMap(List<SystemLog> logs) {
        List<Long> userIds = logs.stream()
                .map(SystemLog::getUserId)
                .filter(id -> id != null)
                .distinct()
                .toList();
        if (userIds.isEmpty()) {
            return Collections.emptyMap();
        }

        return sysUserMapper.selectBatchIds(userIds).stream()
                .collect(Collectors.toMap(SysUser::getId, Function.identity(), (left, right) -> left));
    }

    private SystemLogPageResponse.SystemLogItem toItem(SystemLog log, SysUser user) {
        SystemLogPageResponse.SystemLogItem item = new SystemLogPageResponse.SystemLogItem();
        item.setId(log.getId());
        item.setCompanyId(log.getCompanyId());
        item.setUserId(log.getUserId());
        item.setOperatorName(user != null ? user.getName() : null);
        item.setOperatorUsername(user != null ? user.getUsername() : null);
        item.setModule(log.getModule());
        item.setAction(log.getAction());
        item.setDetails(log.getDetails());
        item.setIp(log.getIp());
        item.setUserAgent(log.getUserAgent());
        item.setCreatedAt(log.getCreatedAt());
        return item;
    }

    private SystemLogPageResponse emptyPage(long page, long size) {
        SystemLogPageResponse response = new SystemLogPageResponse();
        response.setCurrent(page);
        response.setSize(size);
        response.setTotal(0);
        response.setRecords(Collections.emptyList());
        return response;
    }
}
