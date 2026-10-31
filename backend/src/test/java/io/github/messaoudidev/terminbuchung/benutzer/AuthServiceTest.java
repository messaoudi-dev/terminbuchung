package io.github.messaoudidev.terminbuchung.benutzer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import io.github.messaoudidev.terminbuchung.security.TokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    // Stärke 4 statt 10 hält die Tests schnell; das Verhalten ist dasselbe.
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(4);

    @Mock
    private BenutzerRepository benutzerRepository;

    @Mock
    private TokenService tokenService;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(benutzerRepository, new BenutzerMapper(), passwordEncoder, tokenService);
    }

    @Test
    void registrieren_neueEmail_speichertPatientMitBcryptHash() {
        // Arrange
        when(benutzerRepository.existsByEmail("anna@example.com")).thenReturn(false);
        when(benutzerRepository.save(any(Benutzer.class))).thenAnswer(aufruf -> mitId(aufruf.getArgument(0), 1L));

        // Act
        BenutzerResponse ergebnis = authService.registrieren(new RegisterRequest("anna@example.com", "geheim123"));

        // Assert
        ArgumentCaptor<Benutzer> captor = ArgumentCaptor.forClass(Benutzer.class);
        verify(benutzerRepository).save(captor.capture());
        Benutzer gespeichert = captor.getValue();
        assertThat(gespeichert.getRolle()).isEqualTo(Rolle.PATIENT);
        assertThat(gespeichert.getPasswortHash()).isNotEqualTo("geheim123");
        assertThat(passwordEncoder.matches("geheim123", gespeichert.getPasswortHash())).isTrue();
        assertThat(ergebnis).isEqualTo(new BenutzerResponse(1L, "anna@example.com", Rolle.PATIENT));
    }

    @Test
    void registrieren_emailMitGrossbuchstabenUndLeerzeichen_speichertNormalisierteEmail() {
        // Arrange
        when(benutzerRepository.existsByEmail("anna@example.com")).thenReturn(false);
        when(benutzerRepository.save(any(Benutzer.class))).thenAnswer(aufruf -> mitId(aufruf.getArgument(0), 1L));

        // Act
        BenutzerResponse ergebnis = authService.registrieren(new RegisterRequest(" Anna@Example.COM ", "geheim123"));

        // Assert
        assertThat(ergebnis.email()).isEqualTo("anna@example.com");
    }

    @Test
    void registrieren_emailVergeben_wirftExceptionUndSpeichertNicht() {
        // Arrange
        when(benutzerRepository.existsByEmail("anna@example.com")).thenReturn(true);

        // Act + Assert
        assertThatThrownBy(() -> authService.registrieren(new RegisterRequest("anna@example.com", "geheim123")))
                .isInstanceOf(EmailBereitsVergebenException.class);
        verify(benutzerRepository, never()).save(any());
    }

    @Test
    void login_korrekteDaten_liefertTokenMitGueltigkeit() {
        // Arrange
        Benutzer benutzer = mitId(new Benutzer("anna@example.com", passwordEncoder.encode("geheim123"), Rolle.PATIENT), 1L);
        when(benutzerRepository.findByEmail("anna@example.com")).thenReturn(Optional.of(benutzer));
        when(tokenService.erstelleToken(benutzer)).thenReturn("token-123");

        // Act
        LoginResponse ergebnis = authService.login(new LoginRequest("Anna@example.com", "geheim123"));

        // Assert
        assertThat(ergebnis).isEqualTo(new LoginResponse("token-123", 3600));
    }

    @Test
    void login_unbekannteEmail_wirftException() {
        // Arrange
        when(benutzerRepository.findByEmail("unbekannt@example.com")).thenReturn(Optional.empty());

        // Act + Assert
        assertThatThrownBy(() -> authService.login(new LoginRequest("unbekannt@example.com", "geheim123")))
                .isInstanceOf(UngueltigeAnmeldedatenException.class);
        verify(tokenService, never()).erstelleToken(any());
    }

    @Test
    void login_falschesPasswort_wirftException() {
        // Arrange
        Benutzer benutzer = mitId(new Benutzer("anna@example.com", passwordEncoder.encode("geheim123"), Rolle.PATIENT), 1L);
        when(benutzerRepository.findByEmail("anna@example.com")).thenReturn(Optional.of(benutzer));

        // Act + Assert
        assertThatThrownBy(() -> authService.login(new LoginRequest("anna@example.com", "falsch123")))
                .isInstanceOf(UngueltigeAnmeldedatenException.class);
        verify(tokenService, never()).erstelleToken(any());
    }

    @Test
    void legePraxisAnFallsNichtVorhanden_nochNichtVorhanden_speichertPraxisBenutzer() {
        // Arrange
        when(benutzerRepository.existsByEmail("praxis@example.com")).thenReturn(false);

        // Act
        authService.legePraxisAnFallsNichtVorhanden("praxis@example.com", "praxis123");

        // Assert
        ArgumentCaptor<Benutzer> captor = ArgumentCaptor.forClass(Benutzer.class);
        verify(benutzerRepository).save(captor.capture());
        assertThat(captor.getValue().getRolle()).isEqualTo(Rolle.PRAXIS);
        assertThat(passwordEncoder.matches("praxis123", captor.getValue().getPasswortHash())).isTrue();
    }

    @Test
    void legePraxisAnFallsNichtVorhanden_schonVorhanden_speichertNicht() {
        // Arrange
        when(benutzerRepository.existsByEmail("praxis@example.com")).thenReturn(true);

        // Act
        authService.legePraxisAnFallsNichtVorhanden("praxis@example.com", "praxis123");

        // Assert
        verify(benutzerRepository, never()).save(any());
    }

    @Test
    void legePraxisAnFallsNichtVorhanden_emailLeer_wirftException() {
        // Act + Assert
        assertThatThrownBy(() -> authService.legePraxisAnFallsNichtVorhanden("  ", "praxis123"))
                .isInstanceOf(IllegalStateException.class);
        verify(benutzerRepository, never()).save(any());
    }

    private Benutzer mitId(Benutzer benutzer, Long id) {
        ReflectionTestUtils.setField(benutzer, "id", id);
        return benutzer;
    }
}
