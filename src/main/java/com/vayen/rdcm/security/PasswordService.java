package com.vayen.rdcm.security;

import cn.dev33.satoken.secure.BCrypt;
import org.springframework.stereotype.Service;

@Service
public class PasswordService {

    public String encode(String rawPassword) {
        return BCrypt.hashpw(rawPassword);
    }

    public boolean matches(String rawPassword, String encodedPassword) {
        if (rawPassword == null || encodedPassword == null || encodedPassword.isBlank()) {
            return false;
        }
        return BCrypt.checkpw(rawPassword, encodedPassword);
    }
}
