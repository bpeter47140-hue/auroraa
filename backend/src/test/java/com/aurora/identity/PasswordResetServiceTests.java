package com.aurora.identity;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class PasswordResetServiceTests {

    @Test
    void requestUsesGenericResponsePathAndStoresOnlyTokenHash() throws Exception {
        UserAccount user = activeUser();
        UserAccountRepository users = mock(UserAccountRepository.class);
        PasswordResetTokenRepository tokens = mock(PasswordResetTokenRepository.class);
        AuthSessionRepository sessions = mock(AuthSessionRepository.class);
        PasswordEncoder encoder = mock(PasswordEncoder.class);
        InvitationEmailService email = mock(InvitationEmailService.class);
        when(users.findByEmail("person@example.com")).thenReturn(Optional.of(user));
        when(tokens.findAllByUser_IdAndUsedAtIsNull(anyLong())).thenReturn(List.of());

        new PasswordResetService(users, tokens, sessions, encoder, email).requestReset(" PERSON@EXAMPLE.COM ");

        var captor = org.mockito.ArgumentCaptor.forClass(PasswordResetToken.class);
        verify(tokens).save(captor.capture());
        PasswordResetToken saved = captor.getValue();
        org.mockito.ArgumentCaptor<String> rawToken = org.mockito.ArgumentCaptor.forClass(String.class);
        verify(email).sendPasswordReset(eq("person@example.com"), rawToken.capture(), any(Instant.class));
        assertThat(saved.getTokenHash()).isEqualTo(sha256(rawToken.getValue()));
        assertThat(saved.getTokenHash()).isNotEqualTo(rawToken.getValue());
        assertThat(rawToken.getValue()).hasSize(43);
        assertThat(saved.getExpiresAt()).isAfter(Instant.now().plusSeconds(29 * 60));
        assertThat(saved.getExpiresAt()).isBefore(Instant.now().plusSeconds(31 * 60));
    }

    @Test
    void unknownAccountDoesNotSendEmailOrCreateResetToken() {
        UserAccountRepository users = mock(UserAccountRepository.class);
        PasswordResetTokenRepository tokens = mock(PasswordResetTokenRepository.class);
        InvitationEmailService email = mock(InvitationEmailService.class);
        when(users.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

        new PasswordResetService(users, tokens, mock(AuthSessionRepository.class),
            mock(PasswordEncoder.class), email).requestReset("unknown@example.com");

        verifyNoInteractions(tokens, email);
    }

    @Test
    void successfulResetChangesPasswordConsumesTokenAndRevokesSessions() throws Exception {
        UserAccount user = activeUser();
        String raw = "a".repeat(43);
        PasswordResetToken token = new PasswordResetToken(user, sha256(raw), Instant.now().plusSeconds(600));
        AuthSession session = new AuthSession(user, "b".repeat(64), Instant.now().plusSeconds(3600));
        PasswordResetTokenRepository tokens = mock(PasswordResetTokenRepository.class);
        AuthSessionRepository sessions = mock(AuthSessionRepository.class);
        PasswordEncoder encoder = mock(PasswordEncoder.class);
        when(tokens.findByTokenHash(sha256(raw))).thenReturn(Optional.of(token));
        when(tokens.findAllByUser_IdAndUsedAtIsNull(anyLong())).thenReturn(List.of());
        when(sessions.findAllByUser_Id(anyLong())).thenReturn(List.of(session));
        when(encoder.encode("a-new-long-password")).thenReturn("encoded-new-password");

        new PasswordResetService(mock(UserAccountRepository.class), tokens, sessions, encoder,
            mock(InvitationEmailService.class)).completeReset(raw, "a-new-long-password");

        verify(user).changePasswordHash("encoded-new-password");
        assertThat(token.getUsedAt()).isNotNull();
        assertThat(session.getRevokedAt()).isNotNull();
    }

    @Test
    void usedResetTokenCannotBeReused() throws Exception {
        UserAccount user = activeUser();
        String raw = "c".repeat(43);
        PasswordResetToken token = new PasswordResetToken(user, sha256(raw), Instant.now().plusSeconds(600));
        token.markUsed();
        PasswordResetTokenRepository tokens = mock(PasswordResetTokenRepository.class);
        when(tokens.findByTokenHash(sha256(raw))).thenReturn(Optional.of(token));

        assertThatThrownBy(() -> new PasswordResetService(mock(UserAccountRepository.class), tokens,
            mock(AuthSessionRepository.class), mock(PasswordEncoder.class),
            mock(InvitationEmailService.class)).completeReset(raw, "a-new-long-password"))
            .isInstanceOf(org.springframework.web.server.ResponseStatusException.class)
            .hasMessageContaining("400");
    }

    private static UserAccount activeUser() {
        UserAccount user = mock(UserAccount.class);
        when(user.getId()).thenReturn(42L);
        when(user.getEmail()).thenReturn("person@example.com");
        when(user.getStatus()).thenReturn(AccountStatus.ACTIVE);
        return user;
    }

    private static String sha256(String value) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
            .digest(value.getBytes(StandardCharsets.UTF_8)));
    }
}
