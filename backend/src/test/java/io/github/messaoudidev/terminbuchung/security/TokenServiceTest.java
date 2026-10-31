package io.github.messaoudidev.terminbuchung.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

import io.github.messaoudidev.terminbuchung.benutzer.Benutzer;
import io.github.messaoudidev.terminbuchung.benutzer.Rolle;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.test.util.ReflectionTestUtils;

class TokenServiceTest {

    private static final String SECRET = "test-secret-mit-mindestens-32-zeichen-laenge";

    @Test
    void erstelleToken_praxisBenutzer_enthaeltIdRolleUndAblaufNachEinerStunde() {
        // Arrange
        Instant jetzt = Instant.parse("2026-11-02T08:00:00Z");
        Clock clock = Clock.fixed(jetzt, ZoneId.of("Europe/Berlin"));
        JwtConfig jwtConfig = new JwtConfig(SECRET);
        TokenService tokenService = new TokenService(jwtConfig.jwtEncoder(), clock);
        Benutzer benutzer = new Benutzer("praxis@example.com", "hash", Rolle.PRAXIS);
        ReflectionTestUtils.setField(benutzer, "id", 7L);

        // Act
        String token = tokenService.erstelleToken(benutzer);

        // Assert: ohne Prüfung des Ablaufs dekodieren, weil die feste Uhr in der Vergangenheit liegen kann.
        NimbusJwtDecoder decoder = (NimbusJwtDecoder) jwtConfig.jwtDecoder();
        decoder.setJwtValidator(jwt -> OAuth2TokenValidatorResult.success());
        Jwt jwt = decoder.decode(token);
        assertThat(jwt.getSubject()).isEqualTo("7");
        assertThat(jwt.getClaimAsString("rolle")).isEqualTo("PRAXIS");
        assertThat(jwt.getIssuedAt()).isEqualTo(jetzt);
        assertThat(jwt.getExpiresAt()).isEqualTo(Instant.parse("2026-11-02T09:00:00Z"));
    }

    @Test
    void jwtConfig_secretKuerzerAls32Byte_wirftException() {
        // Act + Assert
        assertThatThrownBy(() -> new JwtConfig("zu-kurz"))
                .isInstanceOf(IllegalStateException.class);
    }
}
