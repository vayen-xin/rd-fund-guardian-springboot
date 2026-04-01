package com.vayen.rdcm.common;

import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.exception.NotPermissionException;
import cn.dev33.satoken.exception.NotRoleException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理器
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

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
        return Result.error(403, "无权限访问：" + e.getPermission());
    }

    /**
     * 处理 Sa-Token 无角色异常
     */
    @ExceptionHandler(NotRoleException.class)
    public Result<?> handleNotRole(NotRoleException e) {
        log.warn("无角色访问：{}", e.getMessage());
        return Result.error(403, "无角色访问：" + e.getRole());
    }

    /**
     * 处理业务异常
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public Result<?> handleIllegalArgument(IllegalArgumentException e) {
        log.warn("参数错误：{}", e.getMessage());
        return Result.error(400, e.getMessage());
    }

    /**
     * 处理其他异常
     */
    @ExceptionHandler(Exception.class)
    public Result<?> handleException(Exception e) {
        log.error("系统异常" + e.getMessage(), e);
        return Result.error(500, "系统异常：" + e.getMessage());
    }
}
