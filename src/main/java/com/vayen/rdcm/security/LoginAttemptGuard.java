package com.vayen.rdcm.security;

import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class LoginAttemptGuard {

    private final LoginSecurityProperties properties;
    private final Map<String, AttemptState> attemptStateMap = new ConcurrentHashMap<>();

    public LoginAttemptGuard(LoginSecurityProperties properties) {
        this.properties = properties;
    }

    public LocalDateTime checkBlocked(String ip) {
        if (!properties.isEnabled() || ip == null || ip.isBlank()) {
            return null;
        }
        AttemptState state = attemptStateMap.get(ip);
        if (state == null) {
            return null;
        }
        LocalDateTime now = LocalDateTime.now();
        if (state.lockedUntil != null && state.lockedUntil.isAfter(now)) {
            return state.lockedUntil;
        }
        if (state.lockedUntil != null && !state.lockedUntil.isAfter(now)) {
            attemptStateMap.remove(ip);
        }
        return null;
    }

    public void recordSuccess(String ip) {
        if (!properties.isEnabled() || ip == null || ip.isBlank()) {
            return;
        }
        attemptStateMap.remove(ip);
    }

    public LocalDateTime recordFailure(String ip) {
        if (!properties.isEnabled() || ip == null || ip.isBlank()) {
            return null;
        }
        LocalDateTime now = LocalDateTime.now();
        AttemptState nextState = attemptStateMap.compute(ip, (key, state) -> {
            AttemptState current = state;
            if (current == null || current.windowStartedAt == null || current.windowStartedAt.plusSeconds(properties.getWindowSeconds()).isBefore(now)) {
                current = new AttemptState();
                current.windowStartedAt = now;
                current.failCount = 0;
            }
            current.failCount++;
            if (current.failCount >= properties.getMaxFailures()) {
                current.lockedUntil = now.plusSeconds(properties.getLockSeconds());
            }
            return current;
        });
        return nextState == null ? null : nextState.lockedUntil;
    }

    private static class AttemptState {
        private int failCount;
        private LocalDateTime windowStartedAt;
        private LocalDateTime lockedUntil;
    }
}
