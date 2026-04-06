package com.vayen.rdcm.security;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "app.security.login-limit")
public class LoginSecurityProperties {

    private boolean enabled = true;

    private int maxFailures = 8;

    private int windowSeconds = 300;

    private int lockSeconds = 900;
}
