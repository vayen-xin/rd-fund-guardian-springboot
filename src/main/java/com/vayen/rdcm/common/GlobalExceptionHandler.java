package com.vayen.rdcm.common;

import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.exception.NotPermissionException;
import cn.dev33.satoken.exception.NotRoleException;
import com.vayen.rdcm.security.CurrentUser;
import com.vayen.rdcm.security.CurrentUserService;
import com.vayen.rdcm.service.SystemLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理器
 */
@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    private final CurrentUserService currentUserService;
    private final SystemLogService systemLogService;

    /**
     * 处理 Sa-Token 未登录异常
     */
    @ExceptionHandler(NotLoginException.class)
    public Result<?> handleNotLogin(NotLoginException e) {
        log.warn("未登录访问：{}", e.getMessage());
        return Result.error(401, "未登录或 token 已过期");
    }

    /**
     * 处理 Sa-Token 无权限异常
     */
    @ExceptionHandler(NotPermissionException.class)
    public Result<?> handleNotPermission(NotPermissionException e) {
        log.warn("无权限访问：{}", e.getMessage());
        recordDenied("权限校验", "无权限访问", e.getPermission());
        return Result.error(403, "无权限访问：" + e.getPermission());
    }

    /**
     * 处理 Sa-Token 无角色异常
     */
    @ExceptionHandler(NotRoleException.class)
    public Result<?> handleNotRole(NotRoleException e) {
        log.warn("无角色访问：{}", e.getMessage());
        recordDenied("角色校验", "无角色访问", e.getRole());
        return Result.error(403, "无角色访问：" + e.getRole());
    }

    /**
     * 处理业务参数异常
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public Result<?> handleIllegalArgument(IllegalArgumentException e) {
        log.warn("参数错误：{}", e.getMessage());
        return Result.error(400, e.getMessage());
    }

    /**
     * 处理兜底异常
     */
    @ExceptionHandler(Exception.class)
    public Result<?> handleException(Exception e) {
        log.error("系统异常：{}", e.getMessage(), e);
        return Result.error(500, "系统异常，请联系管理员");
    }

    private void recordDenied(String module, String action, String target) {
        CurrentUser currentUser = currentUserService.getCurrentUserOrNull();
        if (currentUser == null) {
            return;
        }
        systemLogService.record(currentUser, module, action, target, SystemLogService.STATUS_DENIED, "权限不足");
    }
}
