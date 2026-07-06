package com.smarted.ed.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.UnsupportedEncodingException;

@Service
public class EmailServiceImpl implements EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailServiceImpl.class);

    @Autowired
    private JavaMailSender mailSender;

    @Value("${app.auth.verification-url}")
    private String verificationUrlBase;

    @Value("${spring.mail.username}")
    private String fromAddress;

    @Value("${app.mail.provider:smtp}")
    private String mailProvider;

    @Value("${app.resend.enabled:false}")
    private boolean resendEnabled;

    @Value("${app.resend.key:}")
    private String resendApiKey;

    @Value("${app.resend.url:https://api.resend.com/emails}")
    private String resendApiUrl;

    @Value("${app.resend.from:onboarding@resend.dev}")
    private String resendFrom;

    @Value("${app.resend.sender-name:SmartEd Support Team}")
    private String resendSenderName;

    @jakarta.annotation.PostConstruct
    public void init() {
        System.out.println("DEBUG MAIL PROVIDER CONFIG: " + mailProvider);
        System.out.println("DEBUG RESEND ENABLED: " + resendEnabled);
        System.out.println("DEBUG RESEND KEY: " + (resendApiKey != null && !resendApiKey.isBlank() ? "ĐÃ NHẬN KEY (" + resendApiKey.substring(0, Math.min(10, resendApiKey.length())) + "...)" : "KEY BỊ NULL HOẶC RỖNG"));
        System.out.println("DEBUG SUPPORT EMAIL: " + fromAddress);
        System.out.println("DEBUG ACTIVE MAIL MODE: " + (shouldSubmitViaResend() ? "RESEND API" : "LOCAL SMTP (GMAIL)"));
    }

    private boolean shouldSubmitViaResend() {
        // Automatically use Resend on production Render if key is present, OR if enabled explicitly, OR if provider is explicitly set to "resend"
        return resendEnabled || "resend".equalsIgnoreCase(mailProvider) || 
               (System.getenv("RENDER") != null && resendApiKey != null && !resendApiKey.isBlank());
    }

    private void sendViaResend(String toEmail, String subject, String htmlContent) {
        log.info("Bắt đầu gửi email qua Resend HTTP API tới: {}", toEmail);
        try {
            org.springframework.web.client.RestTemplate restTemplate = new org.springframework.web.client.RestTemplate();
            
            java.util.Map<String, Object> requestBody = new java.util.HashMap<>();
            
            // Format from address: "Name <email>"
            String fromField = resendSenderName + " <" + resendFrom + ">";
            requestBody.put("from", fromField);
            
            java.util.List<String> toList = java.util.Collections.singletonList(toEmail);
            requestBody.put("to", toList);
            
            requestBody.put("subject", subject);
            requestBody.put("html", htmlContent);
            
            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + resendApiKey);
            headers.setAccept(java.util.Collections.singletonList(org.springframework.http.MediaType.APPLICATION_JSON));
            
            org.springframework.http.HttpEntity<java.util.Map<String, Object>> entity = new org.springframework.http.HttpEntity<>(requestBody, headers);
            
            org.springframework.http.ResponseEntity<String> response = restTemplate.postForEntity(
                resendApiUrl,
                entity,
                String.class
            );
            
            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("Đã gửi email qua Resend HTTP API thành công tới: {}", toEmail);
            } else {
                log.error("Gửi email qua Resend thất bại. Mã lỗi: {}, Chi tiết: {}", response.getStatusCode(), response.getBody());
            }
        } catch (org.springframework.web.client.RestClientResponseException e) {
            log.error("LỖI gọi API Resend (REST): Mã status = {}, Nội dung phản hồi lỗi: {}", 
                      e.getStatusCode(), e.getResponseBodyAsString(), e);
        } catch (Exception e) {
            log.error("Đã xảy ra lỗi khi gửi email qua Resend: {}", e.getMessage(), e);
        }
    }

    @Override
    @Async
    public void sendVerificationEmail(String toEmail, String token, String fullName) {
        log.info("Bắt đầu gửi email xác thực tới: {}", toEmail);
        String verificationUrl = verificationUrlBase + "?token=" + token;

        String senderName = "SmartEd Support Team";
        String subject = "Xác nhận địa chỉ email của bạn - SmartEd";
        
        String content = "<div style=\"font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 20px; border: 1px solid #e0e0e0; border-radius: 8px; background-color: #ffffff;\">"
                + "<div style=\"text-align: center; margin-bottom: 20px;\">"
                + "<h2 style=\"color: #4A90E2; margin: 0;\">SmartEd</h2>"
                + "<p style=\"font-size: 14px; color: #888888; margin: 5px 0 0 0;\">Học tập thông minh hơn mỗi ngày</p>"
                + "</div>"
                + "<hr style=\"border: none; border-top: 1px solid #eeeeee; margin-bottom: 20px;\" />"
                + "<p>Xin chào <strong>" + fullName + "</strong>,</p>"
                + "<p>Cảm ơn bạn đã đăng ký tài khoản tại <strong>SmartEd</strong>. Để bắt đầu sử dụng dịch vụ của chúng tôi, bạn vui lòng xác nhận địa chỉ email của mình bằng cách nhấp vào nút bên dưới:</p>"
                + "<div style=\"text-align: center; margin: 30px 0;\">"
                + "<a href=\"" + verificationUrl + "\" style=\"background-color: #4A90E2; color: white; padding: 12px 25px; text-decoration: none; border-radius: 5px; font-weight: bold; display: inline-block;\">Xác nhận email</a>"
                + "</div>"
                + "<p>Đường dẫn này có hiệu lực trong vòng <strong>24 giờ</strong>. Nếu nút trên không hoạt động, bạn có thể sao chép và dán liên kết sau vào trình duyệt của mình:</p>"
                + "<p style=\"word-break: break-all; color: #4A90E2;\"><a href=\"" + verificationUrl + "\">" + verificationUrl + "</a></p>"
                + "<hr style=\"border: none; border-top: 1px solid #eeeeee; margin-top: 30px; margin-bottom: 20px;\" />"
                + "<p style=\"font-size: 12px; color: #888888; text-align: center;\">Nếu bạn không thực hiện yêu cầu này, vui lòng bỏ qua email này.<br />&copy; 2026 SmartEd. All rights reserved.</p>"
                + "</div>";

        if (shouldSubmitViaResend()) {
            sendViaResend(toEmail, subject, content);
            return;
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromAddress, senderName);
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(content, true);

            mailSender.send(message);
            log.info("Gửi email xác thực thành công tới: {}", toEmail);
        } catch (MessagingException | UnsupportedEncodingException | MailException e) {
            log.error("LỖI gửi email xác thực tới {}: {}", toEmail, e.getMessage(), e);
        }
    }

    @Override
    @Async
    public void sendEmail(String toEmail, String subject, String htmlContent) {
        log.info("Bắt đầu gửi email thông báo tới: {}", toEmail);
        
        if (shouldSubmitViaResend()) {
            sendViaResend(toEmail, subject, htmlContent);
            return;
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromAddress, "SmartEd Notification");
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("Gửi email thông báo thành công tới: {}", toEmail);
        } catch (Exception e) {
            log.error("LỖI gửi email thông báo tới {}: {}", toEmail, e.getMessage(), e);
        }
    }
}
