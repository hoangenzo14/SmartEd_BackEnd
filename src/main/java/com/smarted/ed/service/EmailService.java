package com.smarted.ed.service;

public interface EmailService {
    void sendVerificationEmail(String toEmail, String token, String fullName);
    void sendEmail(String toEmail, String subject, String htmlContent);
}
