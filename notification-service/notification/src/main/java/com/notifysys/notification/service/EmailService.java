package com.notifysys.notification.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import jakarta.mail.internet.MimeMessage;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    @Async("emailTaskExecutor")
    public void sendWelcomeEmail(String to, String name, Map<String, Object> payload) {
        String subject = "Welcome to NotifySys, " + name + "! 🎉";
        String body = buildWelcomeEmailHtml(name, payload);
        sendHtmlEmail(to, subject, body);
    }

    @Async("emailTaskExecutor")
    public void sendPasswordResetEmail(String to, String name, Map<String, Object> payload) {
        String subject = "Password Reset Request - NotifySys";
        String resetUrl = (String) payload.getOrDefault("resetUrl", "#");
        int expiryMinutes = (int) payload.getOrDefault("expiryMinutes", 15);
        String body = buildPasswordResetHtml(name, resetUrl, expiryMinutes);
        sendHtmlEmail(to, subject, body);
    }

    @Async("emailTaskExecutor")
    public void sendLoginAlertEmail(String to, String name, Map<String, Object> payload) {
        String subject = "New Login to Your NotifySys Account";
        String ipAddress = (String) payload.getOrDefault("ipAddress", "Unknown");
        String loginTime = (String) payload.getOrDefault("loginTime", "Unknown");
        String body = buildLoginAlertHtml(name, ipAddress, loginTime);
        sendHtmlEmail(to, subject, body);
    }

    private void sendHtmlEmail(String to, String subject, String htmlBody) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);
            helper.setFrom("noreply@notifysys.com", "NotifySys");
            mailSender.send(message);
            log.info("Email sent successfully to: {}", to);
        } catch (Exception e) {
            log.error("Failed to send email to {}: {}", to, e.getMessage(), e);
            throw new RuntimeException("Email delivery failed", e);
        }
    }

    private String buildWelcomeEmailHtml(String name, Map<String, Object> payload) {
        String loginUrl = (String) payload.getOrDefault("loginUrl", "https://app.notifysys.com");
        return """
            <!DOCTYPE html>
            <html>
            <body style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 20px;">
                <div style="background: #1a1a2e; color: white; padding: 30px; border-radius: 10px 10px 0 0; text-align: center;">
                    <h1 style="margin: 0;">Welcome to NotifySys!</h1>
                </div>
                <div style="background: #f8f9fa; padding: 30px; border-radius: 0 0 10px 10px;">
                    <h2>Hi %s,</h2>
                    <p>Your account has been created successfully. You can now:</p>
                    <ul>
                        <li>Receive real-time notifications</li>
                        <li>Manage your notification preferences</li>
                        <li>View your notification history</li>
                    </ul>
                    <div style="text-align: center; margin: 30px 0;">
                        <a href="%s" style="background: #e94560; color: white; padding: 12px 30px;
                           border-radius: 5px; text-decoration: none; font-weight: bold;">
                            Get Started
                        </a>
                    </div>
                    <p style="color: #666; font-size: 12px;">If you didn't create this account, please ignore this email.</p>
                </div>
            </body>
            </html>
            """.formatted(name, loginUrl);
    }

    private String buildPasswordResetHtml(String name, String resetUrl, int expiryMinutes) {
        return """
            <!DOCTYPE html>
            <html>
            <body style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 20px;">
                <div style="background: #ff6b35; color: white; padding: 30px; border-radius: 10px 10px 0 0; text-align: center;">
                    <h1>Password Reset</h1>
                </div>
                <div style="background: #f8f9fa; padding: 30px; border-radius: 0 0 10px 10px;">
                    <h2>Hi %s,</h2>
                    <p>We received a request to reset your password. Click the button below to proceed:</p>
                    <div style="text-align: center; margin: 30px 0;">
                        <a href="%s" style="background: #ff6b35; color: white; padding: 12px 30px;
                           border-radius: 5px; text-decoration: none; font-weight: bold;">
                            Reset Password
                        </a>
                    </div>
                    <p><strong>This link expires in %d minutes.</strong></p>
                    <p style="color: #666; font-size: 12px;">If you didn't request this, your account is safe. Please ignore this email.</p>
                </div>
            </body>
            </html>
            """.formatted(name, resetUrl, expiryMinutes);
    }

    private String buildLoginAlertHtml(String name, String ipAddress, String loginTime) {
        return """
            <!DOCTYPE html>
            <html>
            <body style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 20px;">
                <div style="background: #2d6a4f; color: white; padding: 30px; border-radius: 10px 10px 0 0; text-align: center;">
                    <h1>New Login Detected</h1>
                </div>
                <div style="background: #f8f9fa; padding: 30px; border-radius: 0 0 10px 10px;">
                    <h2>Hi %s,</h2>
                    <p>A new login was detected on your account:</p>
                    <table style="width: 100%%; border-collapse: collapse; margin: 20px 0;">
                        <tr style="background: #e9ecef;">
                            <td style="padding: 10px; border: 1px solid #dee2e6;"><strong>IP Address</strong></td>
                            <td style="padding: 10px; border: 1px solid #dee2e6;">%s</td>
                        </tr>
                        <tr>
                            <td style="padding: 10px; border: 1px solid #dee2e6;"><strong>Login Time</strong></td>
                            <td style="padding: 10px; border: 1px solid #dee2e6;">%s</td>
                        </tr>
                    </table>
                    <p>If this wasn't you, please reset your password immediately.</p>
                </div>
            </body>
            </html>
            """.formatted(name, ipAddress, loginTime);
    }
}
