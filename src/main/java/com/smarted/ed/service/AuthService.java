package com.smarted.ed.service;

import com.smarted.ed.dto.LoginRequest;
import com.smarted.ed.dto.LoginResponse;
import com.smarted.ed.dto.SignupRequest;

public interface AuthService {
    void signup(SignupRequest request);
    void verifyEmail(String token);
    LoginResponse login(LoginRequest request);
    void resendVerificationEmail(String email);
}
