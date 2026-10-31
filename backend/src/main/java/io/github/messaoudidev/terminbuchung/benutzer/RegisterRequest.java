package io.github.messaoudidev.terminbuchung.benutzer;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(

        @NotBlank(message = "E-Mail darf nicht leer sein")
        @Email(message = "E-Mail ist ungültig")
        @Size(max = 255, message = "E-Mail darf höchstens 255 Zeichen lang sein")
        String email,

        // BCrypt verarbeitet höchstens 72 Byte, längere Passwörter lehnt Spring Security ab.
        @NotBlank(message = "Passwort darf nicht leer sein")
        @Size(min = 8, max = 72, message = "Passwort muss zwischen 8 und 72 Zeichen lang sein")
        String passwort
) {
}
