package io.github.messaoudidev.terminbuchung.benutzer;

import java.util.Locale;

import io.github.messaoudidev.terminbuchung.security.TokenService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class AuthService {

    private final BenutzerRepository benutzerRepository;
    private final BenutzerMapper benutzerMapper;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;

    public AuthService(BenutzerRepository benutzerRepository, BenutzerMapper benutzerMapper,
                       PasswordEncoder passwordEncoder, TokenService tokenService) {
        this.benutzerRepository = benutzerRepository;
        this.benutzerMapper = benutzerMapper;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
    }

    // Die Vorab-Prüfung liefert eine verständliche Meldung. Bei gleichzeitigen Anfragen
    // sichert der Unique-Index in der Datenbank ab (DataIntegrityViolationException → 409).
    @Transactional
    public BenutzerResponse registrieren(RegisterRequest request) {
        String email = normalisiere(request.email());
        if (benutzerRepository.existsByEmail(email)) {
            throw new EmailBereitsVergebenException();
        }
        Benutzer benutzer = new Benutzer(email, passwordEncoder.encode(request.passwort()), Rolle.PATIENT);
        return benutzerMapper.toResponse(benutzerRepository.save(benutzer));
    }

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        Benutzer benutzer = benutzerRepository.findByEmail(normalisiere(request.email()))
                .orElseThrow(UngueltigeAnmeldedatenException::new);
        if (!passwordEncoder.matches(request.passwort(), benutzer.getPasswortHash())) {
            throw new UngueltigeAnmeldedatenException();
        }
        String token = tokenService.erstelleToken(benutzer);
        return new LoginResponse(token, TokenService.GUELTIGKEIT.toSeconds());
    }

    @Transactional
    public void legePraxisAnFallsNichtVorhanden(String email, String passwort) {
        if (email == null || email.isBlank() || passwort == null || passwort.isBlank()) {
            throw new IllegalStateException("PRAXIS_EMAIL und PRAXIS_PASSWORD müssen gesetzt sein.");
        }
        String normalisierteEmail = normalisiere(email);
        if (benutzerRepository.existsByEmail(normalisierteEmail)) {
            return;
        }
        benutzerRepository.save(new Benutzer(normalisierteEmail, passwordEncoder.encode(passwort), Rolle.PRAXIS));
        log.info("Praxis-Benutzer {} angelegt", normalisierteEmail);
    }

    // Sonst wären "A@x.de" und "a@x.de" zwei verschiedene Konten.
    private String normalisiere(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
