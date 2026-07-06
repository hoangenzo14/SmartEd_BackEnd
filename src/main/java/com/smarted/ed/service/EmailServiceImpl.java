package com.smarted.ed.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.UnsupportedEncodingException;

@Service
public class EmailServiceImpl implements EmailService {

    @Autowired
    private JavaMailSender mailSender;

    @Value("${app.auth.verification-url}")
    private String verificationUrlBase;

    @Value("${spring.mail.username}")
    private String fromAddress;

    @Override
    @Async
    public void sendVerificationEmail(String toEmail, String token, String fullName) {
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

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromAddress, senderName);
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(content, true);

            mailSender.send(message);
        } catch (MessagingException | UnsupportedEncodingException e) {
            throw new RuntimeException("Lỗi gửi email xác thực: " + e.getMessage(), e);
        }
     }

    @Override
    @Async
    public void sendEmail(String toEmail, String subject, String htmlContent) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromAddress, "SmartEd Notification");
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);

            mailSender.send(message);
        } catch (Exception e) {
            System.err.println("Lỗi khi gửi email thông báo tới " + toEmail + ": " + e.getMessage());
        }
    }
}
