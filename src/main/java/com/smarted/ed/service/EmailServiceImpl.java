package com.smarted.ed.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class EmailServiceImpl implements EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailServiceImpl.class);

    @Value("${app.auth.verification-url}")
    private String verificationUrlBase;

    @Value("${app.brevo.key:}")
    private String brevoApiKey;

    @Value("${app.brevo.url:https://api.brevo.com/v3/smtp/email}")
    private String brevoApiUrl;

    @Value("${app.brevo.from:hoangvietqb1912@gmail.com}")
    private String brevoFrom;

    @Value("${app.brevo.sender-name:SmartEd Support Team}")
    private String brevoSenderName;

    @jakarta.annotation.PostConstruct
    public void init() {
        System.out.println("DEBUG BREVO KEY: " + (brevoApiKey != null && !brevoApiKey.isBlank() ? "ĐÃ NHẬN KEY (" + brevoApiKey.substring(0, Math.min(10, brevoApiKey.length())) + "...)" : "KEY BỊ NULL HOẶC RỖNG"));
        System.out.println("DEBUG BREVO FROM: " + brevoFrom);
    }

    private void sendViaBrevo(String toEmail, String toName, String subject, String htmlContent) {
        log.info("Bắt đầu gửi email qua Brevo HTTP API tới: {}", toEmail);
        try {
            org.springframework.web.client.RestTemplate restTemplate = new org.springframework.web.client.RestTemplate();
            
            java.util.Map<String, Object> requestBody = new java.util.HashMap<>();
            
            java.util.Map<String, String> sender = new java.util.HashMap<>();
            sender.put("name", brevoSenderName);
            sender.put("email", brevoFrom);
            requestBody.put("sender", sender);
            
            java.util.List<java.util.Map<String, String>> toList = new java.util.ArrayList<>();
            java.util.Map<String, String> recipient = new java.util.HashMap<>();
            recipient.put("email", toEmail);
            recipient.put("name", toName);
            toList.add(recipient);
            requestBody.put("to", toList);
            
            requestBody.put("subject", subject);
            requestBody.put("htmlContent", htmlContent);
            
            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);
            headers.set("api-key", brevoApiKey);
            headers.setAccept(java.util.Collections.singletonList(org.springframework.http.MediaType.APPLICATION_JSON));
            
            org.springframework.http.HttpEntity<java.util.Map<String, Object>> entity = new org.springframework.http.HttpEntity<>(requestBody, headers);
            
            org.springframework.http.ResponseEntity<String> response = restTemplate.postForEntity(
                brevoApiUrl,
                entity,
                String.class
            );
            
            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("Đã gửi email qua Brevo HTTP API thành công tới: {}", toEmail);
            } else {
                log.error("Gửi email qua Brevo thất bại. Mã lỗi: {}, Chi tiết: {}", response.getStatusCode(), response.getBody());
            }
        } catch (org.springframework.web.client.RestClientResponseException e) {
            log.error("LỖI gọi API Brevo (REST): Mã status = {}, Nội dung phản hồi lỗi: {}", 
                      e.getStatusCode(), e.getResponseBodyAsString(), e);
        } catch (Exception e) {
            log.error("Đã xảy ra lỗi khi gửi email qua Brevo: {}", e.getMessage(), e);
        }
    }

    @Override
    @Async
    public void sendVerificationEmail(String toEmail, String token, String fullName) {
        log.info("Bắt đầu gửi email xác thực tới: {}", toEmail);
        String verificationUrl = verificationUrlBase + "?token=" + token;

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

        sendViaBrevo(toEmail, fullName, subject, content);
    }

    @Override
    @Async
    public void sendEmail(String toEmail, String subject, String htmlContent) {
        log.info("Bắt đầu gửi email thông báo tới: {}", toEmail);
        sendViaBrevo(toEmail, "SmartEd User", subject, htmlContent);
    }
}
