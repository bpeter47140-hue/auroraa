package com.aurora.identity;

import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

@Service
public class InvitationEmailService {
    private static final Logger log = LoggerFactory.getLogger(InvitationEmailService.class);
    private static final DateTimeFormatter EXPIRY_FORMAT =
        DateTimeFormatter.ofPattern("d MMM uuuu 'at' HH:mm z").withZone(ZoneId.of("UTC"));

    private final JavaMailSender mailSender;
    private final boolean enabled;
    private final String from;
    private final String baseUrl;

    public InvitationEmailService(JavaMailSender mailSender,
                                  @Value("${aurora.mail.enabled:false}") boolean enabled,
                                  @Value("${aurora.mail.from:}") String from,
                                  @Value("${aurora.app.base-url:http://localhost:5173}") String baseUrl) {
        this.mailSender = mailSender;
        this.enabled = enabled;
        this.from = from;
        this.baseUrl = baseUrl.replaceAll("/+$", "");
    }

    public boolean sendInvitation(String email, WorkspaceRole role, String token, Instant expiresAt) {
        if (!enabled || from.isBlank()) {
            return false;
        }

        String link = baseUrl + "/?invite=" + URLEncoder.encode(token, StandardCharsets.UTF_8);
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, StandardCharsets.UTF_8.name());
            helper.setFrom(from);
            helper.setTo(email);
            helper.setSubject("You're invited to join a workspace on AURORA");
            helper.setText("""
                You've been invited to join a workspace on AURORA.

                Your assigned role: %s

                Accept your invitation:
                %s

                This one-time link expires at %s (UTC). If you were not expecting this invitation, you can ignore this email.
                """.formatted(role.name(), link, EXPIRY_FORMAT.format(expiresAt)));
            mailSender.send(message);
            return true;
        } catch (Exception ex) {
            log.warn("Invitation email delivery failed for recipient domain {} ({})",
                domainOf(email), ex.getClass().getSimpleName());
            return false;
        }
    }

    public boolean sendPasswordReset(String email, String token, Instant expiresAt) {
        if (!enabled || from.isBlank()) return false;
        String link = baseUrl + "/?reset=" + URLEncoder.encode(token, StandardCharsets.UTF_8);
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, StandardCharsets.UTF_8.name());
            helper.setFrom(from);
            helper.setTo(email);
            helper.setSubject("Reset your AURORA password");
            helper.setText("""
                A password reset was requested for your AURORA account.

                Set a new password using this one-time link:
                %s

                This link expires at %s (UTC). It can be used once. If you did not request this change, ignore this email; your current password remains unchanged.
                """.formatted(link, EXPIRY_FORMAT.format(expiresAt)));
            mailSender.send(message);
            return true;
        } catch (Exception ex) {
            log.warn("Password reset email delivery failed for recipient domain {} ({})",
                domainOf(email), ex.getClass().getSimpleName());
            return false;
        }
    }

    private static String domainOf(String email) {
        int at = email.lastIndexOf('@');
        return at >= 0 ? email.substring(at + 1) : "unknown";
    }
}
