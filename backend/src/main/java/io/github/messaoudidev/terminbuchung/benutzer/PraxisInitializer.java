package io.github.messaoudidev.terminbuchung.benutzer;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

// Ohne einen ersten Praxis-Benutzer könnte niemand Ärzte anlegen, denn Registrieren erzeugt nur Patienten.
@Component
public class PraxisInitializer implements ApplicationRunner {

    private final AuthService authService;
    private final String email;
    private final String passwort;

    public PraxisInitializer(AuthService authService,
                             @Value("${app.praxis.email}") String email,
                             @Value("${app.praxis.password}") String passwort) {
        this.authService = authService;
        this.email = email;
        this.passwort = passwort;
    }

    @Override
    public void run(ApplicationArguments args) {
        authService.legePraxisAnFallsNichtVorhanden(email, passwort);
    }
}
