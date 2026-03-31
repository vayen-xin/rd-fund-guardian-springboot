package com.vayen.rdcm.service;

import com.vayen.rdcm.controller.AuthController;
import com.vayen.rdcm.dto.LoginResponse;

public interface AuthService {

    LoginResponse login(String username, String password);
}
