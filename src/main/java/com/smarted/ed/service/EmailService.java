package com.smarted.ed.service;

public interface EmailService {
    void sendVerificationEmail(String toEmail, String token, String fullName);
}
