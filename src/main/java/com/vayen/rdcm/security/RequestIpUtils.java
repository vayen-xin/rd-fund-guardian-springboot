package com.vayen.rdcm.security;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.util.StringUtils;

public final class RequestIpUtils {

    private RequestIpUtils() {
    }

    public static String resolveClientIp(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(forwardedFor)) {
            return forwardedFor.split(",")[0].trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        if (StringUtils.hasText(realIp)) {
            return realIp.trim();
        }
        return request.getRemoteAddr();
    }
}
