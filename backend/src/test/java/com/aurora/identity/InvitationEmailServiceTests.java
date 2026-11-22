package com.aurora.identity;

import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Test;
import org.springframework.mail.javamail.JavaMailSender;

import java.time.Instant;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class InvitationEmailServiceTests {

    @Test
    void sendsSingleUseInvitationToConfiguredRecipient() throws Exception {
        JavaMailSender sender = mock(JavaMailSender.class);
        MimeMessage message = new MimeMessage(Session.getInstance(new Properties()));
        when(sender.createMimeMessage()).thenReturn(message);

        InvitationEmailService service = new InvitationEmailService(
            sender, true, "aurora@example.com", "https://aurora.example.com"
        );

        boolean sent = service.sendInvitation(
            "member@example.com", WorkspaceRole.MEMBER, "one-time-token", Instant.parse("2026-11-29T12:00:00Z")
        );

        assertThat(sent).isTrue();
        verify(sender).send(message);
        assertThat(message.getSubject()).isEqualTo("You're invited to join a workspace on AURORA");
        assertThat(message.getFrom()[0].toString()).contains("aurora@example.com");
        assertThat(message.getAllRecipients()[0].toString()).isEqualTo("member@example.com");
        assertThat(message.getContent().toString())
            .contains("https://aurora.example.com/?invite=one-time-token")
            .contains("MEMBER")
            .contains("29 Nov 2026 at 12:00 UTC");
    }

    @Test
    void sendsPasswordResetLinkWithExpiryAndOneTimeWarning() throws Exception {
        JavaMailSender sender = mock(JavaMailSender.class);
        MimeMessage message = new MimeMessage(Session.getInstance(new Properties()));
        when(sender.createMimeMessage()).thenReturn(message);
        InvitationEmailService service = new InvitationEmailService(
            sender, true, "aurora@example.com", "https://aurora.example.com"
        );

        boolean sent = service.sendPasswordReset(
            "member@example.com", "reset-token", Instant.parse("2026-11-22T15:00:00Z")
        );

        assertThat(sent).isTrue();
        verify(sender).send(message);
        assertThat(message.getSubject()).isEqualTo("Reset your AURORA password");
        assertThat(message.getContent().toString())
            .contains("https://aurora.example.com/?reset=reset-token")
            .contains("expires at 22 Nov 2026 at 15:00 UTC")
            .contains("It can be used once");
    }

    @Test
    void doesNotAttemptDeliveryWhenEmailIsDisabled() {
        JavaMailSender sender = mock(JavaMailSender.class);
        InvitationEmailService service = new InvitationEmailService(
            sender, false, "aurora@example.com", "http://localhost:5173"
        );

        assertThat(service.sendInvitation(
            "member@example.com", WorkspaceRole.MEMBER, "token", Instant.now()
        )).isFalse();

        verifyNoInteractions(sender);
    }

    @Test
    void doesNotAttemptDeliveryWhenSenderAddressIsMissing() {
        JavaMailSender sender = mock(JavaMailSender.class);
        InvitationEmailService service = new InvitationEmailService(
            sender, true, " ", "http://localhost:5173"
        );

        assertThat(service.sendInvitation(
            "member@example.com", WorkspaceRole.MEMBER, "token", Instant.now()
        )).isFalse();

        verifyNoInteractions(sender);
    }
}
