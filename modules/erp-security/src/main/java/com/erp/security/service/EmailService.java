package com.erp.security.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.email.from-address:noreply@yourcompany.com}")
    private String fromAddress;

    @Value("${app.email.from-name:ERP System}")
    private String fromName;

    @Value("${server.servlet.context-path:/}")
    private String contextPath;

    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;

    @Async
    public void sendPasswordResetEmail(String toEmail, String firstName, String resetCode) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(String.format("%s <%s>", fromName, fromAddress));
            message.setTo(toEmail);
            message.setSubject("Your ERP Password Reset Code");
            message.setText(buildPasswordResetEmailContent(firstName, resetCode));

            mailSender.send(message);
            log.info("Password reset email sent successfully to: {}", toEmail);

        } catch (Exception e) {
            log.error("Failed to send password reset email to: {}", toEmail, e);
            throw new RuntimeException("Failed to send email", e);
        }
    }

    @Async
    public void sendPasswordResetSuccessEmail(String toEmail, String firstName) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(String.format("%s <%s>", fromName, fromAddress));
            message.setTo(toEmail);
            message.setSubject("Password Reset Successful");
            message.setText(buildPasswordResetSuccessContent(firstName));

            mailSender.send(message);
            log.info("Password reset success email sent to: {}", toEmail);

        } catch (Exception e) {
            log.error("Failed to send password reset success email to: {}", toEmail, e);
        }
    }

    @Async
    public void sendPasswordChangeConfirmationEmail(String toEmail, String firstName) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(String.format("%s <%s>", fromName, fromAddress));
            message.setTo(toEmail);
            message.setSubject("Password Changed Successfully");
            message.setText(buildPasswordChangeConfirmationContent(firstName));

            mailSender.send(message);
            log.info("Password change confirmation email sent to: {}", toEmail);

        } catch (Exception e) {
            log.error("Failed to send password change confirmation email to: {}", toEmail, e);
        }
    }

    @Async
    public void sendEmailChangeVerificationEmail(String toEmail, String firstName, String verificationToken) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(String.format("%s <%s>", fromName, fromAddress));
            message.setTo(toEmail);
            message.setSubject("Verify Your New Email Address");
            message.setText(buildEmailChangeVerificationContent(firstName, verificationToken));

            mailSender.send(message);
            log.info("Email change verification email sent to: {}", toEmail);

        } catch (Exception e) {
            log.error("Failed to send email change verification email to: {}", toEmail, e);
            throw new RuntimeException("Failed to send email", e);
        }
    }

    @Async
    public void sendEmailChangeNotificationEmail(String toEmail, String firstName, String newEmail) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(String.format("%s <%s>", fromName, fromAddress));
            message.setTo(toEmail);
            message.setSubject("Email Change Request - Security Notice");
            message.setText(buildEmailChangeNotificationContent(firstName, newEmail));

            mailSender.send(message);
            log.info("Email change notification sent to: {}", toEmail);

        } catch (Exception e) {
            log.error("Failed to send email change notification to: {}", toEmail, e);
        }
    }

    @Async
    public void sendEmailChangeSuccessEmail(String toEmail, String firstName) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(String.format("%s <%s>", fromName, fromAddress));
            message.setTo(toEmail);
            message.setSubject("Email Address Changed Successfully");
            message.setText(buildEmailChangeSuccessContent(firstName));

            mailSender.send(message);
            log.info("Email change success email sent to: {}", toEmail);

        } catch (Exception e) {
            log.error("Failed to send email change success email to: {}", toEmail, e);
        }
    }

    @Async
    public void sendEmailChangeNotificationOldEmail(String toEmail, String firstName, String newEmail) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(String.format("%s <%s>", fromName, fromAddress));
            message.setTo(toEmail);
            message.setSubject("Your Email Address Has Been Changed");
            message.setText(buildEmailChangeNotificationOldEmailContent(firstName, newEmail));

            mailSender.send(message);
            log.info("Email change notification to old email sent to: {}", toEmail);

        } catch (Exception e) {
            log.error("Failed to send email change notification to old email: {}", toEmail, e);
        }
    }


    private String buildPasswordResetEmailContent(String firstName, String resetCode) {
        return String.format("""
                Hi %s,
                
                You requested a password reset for your ERP account.
                
                Your verification code is: %s
                
                This code will expire in 30 minutes.
                
                If you didn't request this reset, please ignore this email.
                
                Thanks,
                ERP Support Team
                """, firstName, resetCode);
    }

    private String buildPasswordResetSuccessContent(String firstName) {
        return String.format("""
                Hi %s,
                
                Your password has been successfully reset.
                
                If you didn't make this change, please contact our support team immediately.
                
                Thanks,
                ERP Support Team
                """, firstName);
    }

    private String buildPasswordChangeConfirmationContent(String firstName) {
        return String.format("""
                Hi %s,
                
                Your password has been changed successfully.
                
                If you didn't make this change, please contact our support team immediately and consider changing your password again.
                
                For your security, this change was made from your authenticated session.
                
                Thanks,
                ERP Support Team
                """, firstName);
    }

    private String buildEmailChangeVerificationContent(String firstName, String verificationToken) {
        String verificationUrl = baseUrl + contextPath + "api/user/verify-email?token=" + verificationToken;

        return String.format("""
                Hi %s,
                
                You requested to change your email address in your ERP account.
                
                To complete this change, please click the link below or copy and paste it into your browser:
                
                %s
                
                This verification link will expire in 24 hours.
                
                If you didn't request this change, please ignore this email and contact our support team.
                
                Thanks,
                ERP Support Team
                """, firstName, verificationUrl);
    }

    private String buildEmailChangeNotificationContent(String firstName, String newEmail) {
        return String.format("""
                Hi %s,
                
                A request has been made to change your email address to: %s
                
                A verification email has been sent to the new address. The change will only take effect once the new email address is verified.
                
                If you didn't make this request, please:
                1. Change your password immediately
                2. Contact our support team
                
                Your current email address remains active until the change is verified.
                
                Thanks,
                ERP Support Team
                """, firstName, newEmail);
    }

    private String buildEmailChangeSuccessContent(String firstName) {
        return String.format("""
                Hi %s,
                
                Your email address has been successfully updated.
                
                You can now use this email address to log into your ERP account.
                
                If you didn't make this change, please contact our support team immediately.
                
                Thanks,
                ERP Support Team
                """, firstName);
    }

    private String buildEmailChangeNotificationOldEmailContent(String firstName, String newEmail) {
        return String.format("""
                Hi %s,
                
                Your email address has been successfully changed to: %s
                
                This email address is no longer associated with your ERP account.
                
                If you didn't make this change, please contact our support team immediately as your account may have been compromised.
                
                Thanks,
                ERP Support Team
                """, firstName, newEmail);
    }

}
