package com.vayen.rdcm.logging;

import com.vayen.rdcm.security.CurrentUser;
import com.vayen.rdcm.security.CurrentUserService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;

/**
 * 记录控制器方法的应用日志，避免只看到 SQL 噪音。
 */
@Aspect
@Component
@Slf4j
@RequiredArgsConstructor
public class ControllerRequestLogAspect {

    private final CurrentUserService currentUserService;

    @Around("within(@org.springframework.web.bind.annotation.RestController *) && execution(public * com.vayen.rdcm.controller..*(..))")
    public Object logRequest(ProceedingJoinPoint joinPoint) throws Throwable {
        long start = System.currentTimeMillis();
        HttpServletRequest request = currentRequest();
        CurrentUser currentUser = currentUserService.getCurrentUserOrNull();
        String action = resolveAction(joinPoint);
        String method = request == null ? "-" : request.getMethod();
        String uri = request == null ? "-" : request.getRequestURI();

        try {
            Object result = joinPoint.proceed();
            log.info(
                    "[API] {} {} | action={} | user={} | role={} | companyId={} | result=SUCCESS | duration={}ms",
                    method,
                    uri,
                    action,
                    currentUser == null ? "anonymous" : currentUser.getUsername(),
                    currentUser == null ? "-" : currentUser.getRole(),
                    currentUser == null ? "-" : currentUser.getCompanyId(),
                    System.currentTimeMillis() - start
            );
            return result;
        } catch (Throwable throwable) {
            log.warn(
                    "[API] {} {} | action={} | user={} | role={} | companyId={} | result=FAIL | duration={}ms | error={}",
                    method,
                    uri,
                    action,
                    currentUser == null ? "anonymous" : currentUser.getUsername(),
                    currentUser == null ? "-" : currentUser.getRole(),
                    currentUser == null ? "-" : currentUser.getCompanyId(),
                    System.currentTimeMillis() - start,
                    throwable.getMessage()
            );
            throw throwable;
        }
    }

    private String resolveAction(ProceedingJoinPoint joinPoint) {
        Method method = ((MethodSignature) joinPoint.getSignature()).getMethod();
        return method.getDeclaringClass().getSimpleName() + "#" + method.getName();
    }

    private HttpServletRequest currentRequest() {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        if (attributes instanceof ServletRequestAttributes servletRequestAttributes) {
            return servletRequestAttributes.getRequest();
        }
        return null;
    }
}
