package com.vayen.rdcm.audit;

import com.vayen.rdcm.common.Result;
import com.vayen.rdcm.security.CurrentUser;
import com.vayen.rdcm.security.CurrentUserService;
import com.vayen.rdcm.service.SystemLogService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * 写操作成功后统一记录系统日志。
 */
@Aspect
@Component
@RequiredArgsConstructor
public class AuditLogAspect {

    private final SystemLogService systemLogService;
    private final CurrentUserService currentUserService;

    @Around("@annotation(auditLog)")
    public Object around(ProceedingJoinPoint joinPoint, AuditLog auditLog) throws Throwable {
        CurrentUser currentUser = currentUserService.getCurrentUserOrNull();
        try {
            Object result = joinPoint.proceed();
            if (isSuccess(result) && currentUser != null) {
                systemLogService.record(currentUser, auditLog.module(), auditLog.action(), buildDetails(auditLog), SystemLogService.STATUS_SUCCESS, null);
            }
            return result;
        } catch (Throwable throwable) {
            if (currentUser != null) {
                systemLogService.record(currentUser, auditLog.module(), auditLog.action(), buildDetails(auditLog), SystemLogService.STATUS_FAIL, sanitizeMessage(throwable.getMessage()));
            }
            throw throwable;
        }
    }

    private boolean isSuccess(Object result) {
        if (result == null) {
            return true;
        }
        if (result instanceof Result<?> wrapped) {
            return wrapped.getCode() != null && wrapped.getCode() == 200;
        }
        return true;
    }

    private String buildDetails(AuditLog auditLog) {
        if (!auditLog.summary().isBlank()) {
            return auditLog.summary();
        }
        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return auditLog.action();
        }
        HttpServletRequest request = attributes.getRequest();
        String query = request.getQueryString();
        if (query == null || query.isBlank()) {
            return request.getMethod() + " " + request.getRequestURI();
        }
        return request.getMethod() + " " + request.getRequestURI() + "?" + query;
    }

    private String sanitizeMessage(String message) {
        if (message == null || message.isBlank()) {
            return "操作失败";
        }
        return message.length() > 200 ? message.substring(0, 200) : message;
    }
}
