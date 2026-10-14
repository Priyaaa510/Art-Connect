package com.example.ArtConnect.notification;

import java.nio.charset.StandardCharsets;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import jakarta.mail.internet.MimeMessage;

/**
 * Sends invoice emails through any SMTP server configured with spring.mail.* (SMTP_HOST, SMTP_PORT,
 * SMTP_USERNAME, SMTP_PASSWORD), e.g. Gmail with an app password, Brevo, or a Mailtrap test inbox.
 */
@Component
@ConditionalOnProperty(name = "app.notifications.email.provider", havingValue = "smtp")
public class SmtpEmailSender implements EmailSender {
    private final JavaMailSender mailSender;
    private final String from;

    public SmtpEmailSender(JavaMailSender mailSender,
                           @Value("${app.notifications.email.from:ArtConnect <no-reply@artconnect.local>}") String from) {
        this.mailSender = mailSender;
        this.from = from;
    }

    @Override public String provider() { return "smtp"; }
    @Override public boolean simulated() { return false; }

    @Override
    public void send(String to, String subject, String html, String plainText) throws Exception {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name());
        helper.setFrom(from);
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(plainText, html);
        mailSender.send(message);
    }
}
