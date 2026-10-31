package io.github.messaoudidev.terminbuchung.benutzer;

import org.springframework.stereotype.Component;

@Component
public class BenutzerMapper {

    public BenutzerResponse toResponse(Benutzer benutzer) {
        return new BenutzerResponse(benutzer.getId(), benutzer.getEmail(), benutzer.getRolle());
    }
}
