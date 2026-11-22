package com.aurora;

import com.aurora.identity.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class PasswordResetIntegrationTests {
    @Autowired UserAccountRepository users;
    @Autowired PasswordResetTokenRepository resetTokens;
    @Autowired AuthSessionRepository sessions;
    @Autowired PasswordResetService resetService;
    @Autowired PasswordEncoder passwordEncoder;

    @Test
    void resetTokenPersistsAsHashAndSuccessfulResetRevokesSession() throws Exception {
        UserAccount user = users.saveAndFlush(new UserAccount(
            "reset.integration@example.com", passwordEncoder.encode("old-password-value"), "Reset Test"
        ));
        String rawToken = "integration-reset-token-value-1234567890";
        PasswordResetToken token = resetTokens.saveAndFlush(new PasswordResetToken(
            user, sha256(rawToken), Instant.now().plusSeconds(1800)
        ));
        AuthSession session = sessions.saveAndFlush(new AuthSession(
            user, "d".repeat(64), Instant.now().plusSeconds(3600)
        ));

        assertThat(resetTokens.findByTokenHash(sha256(rawToken))).isPresent();
        assertThat(token.getTokenHash()).isNotEqualTo(rawToken);

        resetService.completeReset(rawToken, "a-new-password-value");

        assertThat(passwordEncoder.matches("a-new-password-value", user.getPasswordHash())).isTrue();
        assertThat(token.getUsedAt()).isNotNull();
        assertThat(sessions.findByTokenHash("d".repeat(64)).orElseThrow().getRevokedAt()).isNotNull();
    }

    private static String sha256(String value) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
            .digest(value.getBytes(StandardCharsets.UTF_8)));
    }
}
